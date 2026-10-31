package com.hz6826.xjtunetlogin.session

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.hz6826.xjtunetlogin.data.AccountStore
import com.hz6826.xjtunetlogin.data.SettingsStore
import com.hz6826.xjtunetlogin.model.Account
import com.hz6826.xjtunetlogin.model.CampusState
import com.hz6826.xjtunetlogin.model.LogEntry
import com.hz6826.xjtunetlogin.model.OnlineInfo
import com.hz6826.xjtunetlogin.model.SessionState
import com.hz6826.xjtunetlogin.net.SrunClient
import com.hz6826.xjtunetlogin.net.WifiTransport
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/**
 * 持有一次前台会话，跨屏幕旋转保留；所有认证请求串行执行。
 *
 * 自动自检（30 秒轮询）走 silent 模式：不置 busy、不改 status、默认不写日志，
 * 避免用户输入时输入框被禁用/界面闪烁。
 */
class SessionController(context: Context) {
    private val app = context.applicationContext
    private val accountStore = AccountStore(app)
    private val settingsStore = SettingsStore(app)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    var account = Account()
        private set
    var state = SessionState(busy = true, status = "正在读取账户设置…")
        private set
    var logs: List<LogEntry> = emptyList()
        private set
    var listener: ((SessionState, List<LogEntry>) -> Unit)? = null
    /** 前台保活服务用：只关心状态变化，用来刷新通知。 */
    var stateObserver: ((SessionState) -> Unit)? = null
    /** 一次性提示（Toast），不写进状态文案。 */
    var toastListener: ((String) -> Unit)? = null
    var loaded: (() -> Unit)? = null
    var logHttp = false
        private set
    /** 「前台保活与自动登录」开关（设置里的 keepAlive），控制是否自动登录。 */
    var keepAlive = false
        private set
    private var active = false
    private var uiActive = false
    private var backgroundActive = false
    private var ready = false
    val isReady: Boolean get() = ready
    private var closed = false
    private var registered = false
    private var manualLogout = false
    private var currentNetwork: Network? = null
    private var taskTransport: WifiTransport? = null
    private var taskRunning = false
    private var refreshPending = false
    private var campusMisses = 0
    private var autoLoginAttempts = 0
    private val wifiNetworks = linkedSetOf<Network>()
    private var authenticatedAccount: Account? = null
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (!closed && wifiNetworks.remove(network)) networkChanged()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (closed) return
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                wifiNetworks.add(network)
            } else {
                wifiNetworks.remove(network)
            }
            networkChanged()
        }
    }
    private val poll = object : Runnable {
        override fun run() {
            if (!active || closed) return
            refresh(automatic = true)
            main.postDelayed(this, 30000)
        }
    }

    init {
        // 后台/开机服务拉起时没有 Activity，必须在这里恢复 keepAlive，自动登录才会生效。
        val savedSettings = settingsStore.load()
        logHttp = savedSettings.logHttp
        keepAlive = savedSettings.keepAlive
        executor.execute {
            val result = runCatching { accountStore.load() }
            main.post {
                if (!closed) {
                    account = result.getOrDefault(Account())
                    ready = true
                    state = state.copy(busy = false)
                    if (result.isFailure) publish("已保存账户读取失败，请重新输入并保存", level = "WARN")
                    log("SUCCESS", "启动成功！欢迎来到仙交网络助手(XJTUNetLoginAndroid) by Hz6826")
                    log("INFO", "当前版本: v${runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull().orEmpty()}")
                    log("INFO", "项目地址: https://github.com/Hz6826/XJTUNetLoginAndroid")
                    log(
                        "INFO",
                        "本项目以GPL-3.0开源，完全免费！如果你认为该软件帮助到了你，" +
                            "麻烦在GitHub点个Star，或将软件分享给朋友，你的支持是我继续创作的动力~",
                    )
                    loaded?.invoke()
                    emit()
                    if (active) networkChanged(force = true)
                }
            }
        }
    }

    fun start() {
        uiActive = true
        updateActive()
    }

    fun setBackgroundActive(enabled: Boolean) {
        backgroundActive = enabled
        updateActive()
    }

    private fun updateActive() {
        if (!uiActive && !backgroundActive) {
            active = false
            main.removeCallbacks(poll)
            if (registered) {
                runCatching { connectivity.unregisterNetworkCallback(callback) }
                registered = false
                wifiNetworks.clear()
                currentNetwork = null
            }
            return
        }
        if (closed || active) return
        active = true
        if (!registered) {
            connectivity.registerNetworkCallback(NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback, main)
            registered = true
        }
        if (ready) networkChanged(force = true)
        main.postDelayed(poll, 30000)
    }

    fun stop() {
        uiActive = false
        updateActive()
    }

    private fun networkChanged(force: Boolean = false) {
        val network = wifiNetworks.firstOrNull()
        if (network != currentNetwork) {
            taskTransport?.cancel()
            currentNetwork = network
            manualLogout = false
            authenticatedAccount = null
            campusMisses = 0
            autoLoginAttempts = 0
            state = state.copy(
                online = null,
                campus = if (network == null) CampusState.OffCampus else CampusState.Unknown,
            )
            publish(
                if (network == null) "请先连接校园 Wi-Fi" else "Wi-Fi 已连接，正在查询…",
                level = if (network == null) "WARN" else "INFO",
            )
        } else if (!force) return
        if (network == null) {
            if (state.campus != CampusState.OffCampus || state.online != null) {
                state = state.copy(campus = CampusState.OffCampus, online = null)
                emit()
            }
            if (force) publish("请先连接校园 Wi-Fi", log = false, level = "WARN")
            return
        }
        if (!active || !ready) return
        if (taskRunning) refreshPending = true else refresh(automatic = true)
    }

    fun forgetAccount() {
        if (!ready || state.busy) return
        runTask("清除已保存账户…", needsWifi = false) { _, _, _ ->
            accountStore.save(account.copy(remember = false, autoLogin = false))
            main.post {
                if (!closed) {
                    account = account.copy(remember = false, autoLogin = false)
                    publish("已清除设备上保存的账户", level = "SUCCESS")
                }
            }
        }
    }

    fun save(updated: Account) {
        if (!ready || state.busy) return
        val error = runCatching {
            Account.validatePortal(updated.baseUrl)
            require(updated.acId.matches(Regex("[0-9]+"))) { "AC ID 应为数字" }
        }.exceptionOrNull()
        if (error != null) {
            publish(error.message ?: "账户设置有误", level = "ERROR")
            return
        }
        runTask("保存设置…", needsWifi = false) { _, _, _ ->
            accountStore.save(updated)
            main.post {
                if (!closed) {
                    if (account.baseUrl != updated.baseUrl || account.acId != updated.acId) {
                        state = state.copy(online = null)
                        authenticatedAccount = null
                    }
                    account = updated
                    settleStatus()
                    val message = "设置已保存"
                    log("SUCCESS", message)
                    toastListener?.invoke(message)
                }
            }
        }
    }

    fun login(updated: Account) = login(updated, automatic = false)

    private fun login(updated: Account, automatic: Boolean) {
        if (!ready || state.busy || taskRunning) return
        val error = runCatching { updated.validate() }.exceptionOrNull()
        if (error != null) {
            publish(error.message ?: "账户设置有误", level = "ERROR")
            return
        }
        manualLogout = false
        runTask("开始登录…", baseOverride = updated.baseUrl, silent = automatic) { client, network, _ ->
            if (!automatic) {
                accountStore.save(updated)
                main.post {
                    if (!closed) {
                        account = updated
                    }
                }
            }
            val result = client!!.login(updated) { stage ->
                main.post { if (!closed && network == currentNetwork) publish(stage, log = false) }
            }
            main.post {
                if (!closed && network == currentNetwork) {
                    authenticatedAccount = updated
                    autoLoginAttempts = 0
                    accountStore.markLoggedIn()
                    state = state.copy(online = OnlineInfo.from(result), campus = CampusState.OnCampus)
                    publish("已连接校园网", level = "SUCCESS")
                }
            }
        }
    }

    fun logout(updated: Account) {
        if (state.busy) return
        manualLogout = true
        val identity = authenticatedAccount ?: updated
        val accepted = runTask("正在注销…", baseOverride = identity.baseUrl) { client, _, _ ->
            client!!.logout(identity)
            main.post {
                if (!closed) {
                    authenticatedAccount = null
                    state = state.copy(online = null)
                    settleStatus()
                    val message = "已注销，自动登录已暂停至下次连接 Wi-Fi"
                    log("SUCCESS", message)
                    toastListener?.invoke(message)
                }
            }
        }
        if (!accepted) manualLogout = false
    }

    fun refresh(automatic: Boolean = false) {
        if (!ready || state.busy || taskRunning) return
        if (automatic && (!active || currentNetwork == null)) return
        // 非校园网环境下无需反复查询认证地址（避免每 30 秒一次无谓超时）。
        if (automatic && state.campus == CampusState.OffCampus) return
        val requestNetwork = currentNetwork
        var shouldLogin = false
        runTask(
            label = "查询在线状态…",
            silent = automatic,
            timeoutMs = if (automatic) 2500 else 8000,
            after = {
                if (shouldLogin && active && requestNetwork != null && requestNetwork == currentNetwork &&
                    keepAlive && account.remember && !manualLogout && autoLoginAttempts < 3) {
                    autoLoginAttempts++
                    login(account, automatic = true)
                }
            },
        ) { client, network, transport ->
            try {
                val result = client!!.query()
                val info = OnlineInfo.from(result)
                campusMisses = 0
                main.post {
                    if (!closed && network == currentNetwork) {
                        applySnapshot(info, CampusState.OnCampus, automatic)
                    }
                }
                if (info == null) shouldLogin = true
            } catch (e: Exception) {
                if (network == null) throw e
                val redirect = runCatching { transport!!.isCampusPortalRedirect() }.getOrDefault(false)
                main.post {
                    if (!closed && network == currentNetwork) {
                        if (redirect) {
                            campusMisses = 0
                            applySnapshot(null, CampusState.OnCampus, automatic)
                        } else if (++campusMisses >= 3) {
                            // 连续失败才判定非校园网，避免一次探测超时把自动检测永久关掉。
                            campusMisses = 0
                            applySnapshot(null, CampusState.OffCampus, automatic)
                        }
                    }
                }
                if (!redirect || !keepAlive || !account.remember || manualLogout) throw e
                shouldLogin = true
            }
        }
    }

    /**
     * 刷新在线状态并顺带检测外网连通性。外网结果通过 callback 交给界面用 Toast 提示，
     * 不写进状态文案（状态只表达校园网认证状态）。
     */
    fun refreshAndCheckInternet(onInternetResult: (Boolean) -> Unit) {
        if (!ready || state.busy || taskRunning) return
        runTask("刷新状态并检测外网…") { client, network, transport ->
            var info: OnlineInfo? = null
            var campus = CampusState.OffCampus
            try {
                val result = client!!.query()
                info = OnlineInfo.from(result)
                campus = CampusState.OnCampus
            } catch (e: Exception) {
                if (network != null) {
                    val redirect = runCatching { transport!!.isCampusPortalRedirect() }.getOrDefault(false)
                    campus = if (redirect) CampusState.OnCampus else CampusState.OffCampus
                }
            }
            val reachable = transport?.let { runCatching { it.checkInternet() }.getOrDefault(false) } ?: false
            main.post {
                if (!closed && network == currentNetwork) {
                    applySnapshot(info, campus, silent = false)
                    onInternetResult(reachable)
                }
            }
        }
    }

    fun clearLogs() {
        logs = emptyList()
        emit()
    }

    fun setLogHttp(enabled: Boolean) {
        logHttp = enabled
    }

    fun setKeepAlive(enabled: Boolean) {
        keepAlive = enabled
    }

    /** 供更新检查等外部流程写入运行记录。 */
    fun recordLog(level: String, message: String) {
        log(level, message)
    }

    /** 保活/自动登录是否可用：登录成功过且当前记住了账户。 */
    fun canKeepAlive(): Boolean =
        account.remember && account.username.isNotBlank() && account.password.isNotBlank() &&
            accountStore.hasLoggedInBefore()

    private fun runTask(
        label: String,
        needsWifi: Boolean = true,
        silent: Boolean = false,
        baseOverride: String? = null,
        timeoutMs: Int = 8000,
        after: () -> Unit = {},
        task: (SrunClient?, Network?, WifiTransport?) -> Unit,
    ): Boolean {
        if (closed || state.busy || taskRunning) {
            if (!silent) toastListener?.invoke("正在处理上一个操作，请稍候")
            return false
        }
        val network = if (needsWifi) currentNetwork else null
        if (needsWifi && network == null) {
            if (!silent) publish("请先连接校园 Wi-Fi", level = "WARN")
            return false
        }
        if (!silent) {
            state = state.copy(busy = true)
            publish(label, level = "STEP")
        }
        taskRunning = true
        val baseUrl = baseOverride ?: account.baseUrl
        val transport = network?.let {
            WifiTransport(it, baseUrl, timeoutMs, onHttp = { line ->
                if (logHttp) main.post { if (!closed) log("HTTP", line) }
            })
        }
        taskTransport = transport
        executor.execute {
            try {
                task(transport?.let { SrunClient(it) }, network, transport)
            } catch (e: Exception) {
                if (!silent) {
                    val message = when (e) {
                        is SocketTimeoutException -> "请求超时，请检查校园 Wi-Fi 后重试"
                        is IOException -> "网络请求失败，请检查校园 Wi-Fi 和认证地址"
                        is IllegalArgumentException, is IllegalStateException -> e.message ?: "操作失败"
                        else -> "操作失败，请检查配置后重试"
                    }
                    // 不记录异常堆栈、原始响应或请求 URL，避免暴露认证参数。
                    main.post { if (!closed && (network == null || network == currentNetwork)) publish(message.take(200), level = "ERROR") }
                }
            } finally {
                main.post {
                    if (!closed) {
                        taskTransport = null
                        taskRunning = false
                        if (!silent) {
                            state = state.copy(busy = false)
                            emit()
                        }
                        after()
                        // 静默任务结束后收敛过渡状态；如果 after 触发了自动登录（busy=true）就交给它自己更新。
                        if (silent && !state.busy) settleStatus()
                        if (refreshPending && !state.busy) {
                            refreshPending = false
                            if (active && currentNetwork != null) refresh(automatic = true)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun applySnapshot(info: OnlineInfo?, campus: CampusState, silent: Boolean) {
        // 流量/时长每轮都会变，不能拿整个 OnlineInfo 判断“变化”，否则静默轮询会刷屏日志。
        val changed = (state.online != null) != (info != null) || state.campus != campus
        state = state.copy(online = info, campus = campus)
        if (silent) {
            if (changed) log(if (info != null) "SUCCESS" else "WARN", if (info != null) "已连接校园网" else "校园网未登录")
            // 静默刷新也要把「正在查询…」之类的过渡状态收敛成最终状态，否则会一直卡住。
            settleStatus()
        } else if (changed || state.status != settledStatusText()) {
            publish(settledStatusText(), log = changed, level = if (info != null) "SUCCESS" else "WARN")
        }
    }

    private fun settledStatusText(): String = when {
        state.online != null -> "已连接校园网"
        state.campus == CampusState.OnCampus -> "校园网未登录"
        state.campus == CampusState.OffCampus -> "当前不是校园网环境"
        else -> "正在查询校园网络…"
    }

    private fun settleStatus() {
        val text = settledStatusText()
        if (state.status != text) {
            state = state.copy(status = text)
            emit()
        }
    }

    private fun publish(message: String, log: Boolean = true, level: String = "INFO") {
        state = state.copy(status = message)
        if (log) logs = (logs + LogEntry(level, message)).takeLast(200)
        emit()
    }

    private fun log(level: String, message: String) {
        logs = (logs + LogEntry(level, message)).takeLast(200)
        emit()
    }

    private fun emit() {
        listener?.invoke(state, logs)
        stateObserver?.invoke(state)
    }

    fun close() {
        backgroundActive = false
        stop()
        closed = true
        if (registered) {
            connectivity.unregisterNetworkCallback(callback)
            registered = false
        }
        wifiNetworks.clear()
        currentNetwork = null
        listener = null
        stateObserver = null
        toastListener = null
        loaded = null
        executor.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }
}
