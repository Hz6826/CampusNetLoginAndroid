package com.hz6826.campusnetlogin.remote

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import com.hz6826.campusnetlogin.data.SettingsStore
import com.hz6826.campusnetlogin.data.canReadSsid
import com.hz6826.campusnetlogin.data.currentWifiSsid
import com.hz6826.campusnetlogin.model.CampusState
import com.hz6826.campusnetlogin.session.SessionHolder
import org.json.JSONArray
import org.json.JSONObject

/**
 * 给「岱宗盒子」的遥控器插件用的进程外入口：查状态、登录、注销。
 *
 * 为什么要有它：账号密码用 Android Keystore 加密存在本应用里，别的应用（哪怕同一台机器）
 * 既读不到也不该读；插件能做的只是「让本体去干活」。
 *
 * 通信走 [Messenger]（本质还是 binder），对面不需要共享 AIDL，只要认四种 what。
 * 调用方校验用 `Message.sendingUid`（消息被投递到主线程后 `Binder.getCallingUid()` 已经不可靠了），
 * 只放行盒子本体的两个包名；另外设置里还有一个总开关，关掉连白名单也不认。
 */
class RemoteControlService : Service() {

    private val controller by lazy { SessionHolder.get(applicationContext) }
    private val settings by lazy { SettingsStore(applicationContext) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder = Messenger(MessageHandler()).binder

    override fun onCreate() {
        super.onCreate()
        // 遥控期间保持会话激活：不 start() 的话 currentNetwork 是空的，登录会直接报「请先连接校园 Wi-Fi」。
        controller.start()
    }

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }

    private inner class MessageHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val reply = msg.replyTo ?: return
            try {
                if (!callerAllowed(msg.sendingUid)) {
                    sendError(reply, msg.what, "forbidden")
                    return
                }
                when (msg.what) {
                    MSG_HELLO -> sendJson(reply, msg.what, hello())
                    MSG_STATUS -> {
                        if (!remoteEnabled()) {
                            sendError(reply, msg.what, "remote_disabled")
                        } else {
                            status(reply, msg.what, msg.data ?: Bundle())
                        }
                    }
                    MSG_LOGIN -> {
                        if (!remoteEnabled()) sendError(reply, msg.what, "remote_disabled")
                        else login(reply, msg.what)
                    }
                    MSG_LOGOUT -> {
                        if (!remoteEnabled()) sendError(reply, msg.what, "remote_disabled")
                        else logout(reply, msg.what)
                    }
                    else -> sendError(reply, msg.what, "unknown_command")
                }
            } catch (t: Throwable) {
                sendError(reply, msg.what, "failed")
            }
        }
    }

    // ── 请求处理 ────────────────────────────────────────────────────────────

    private fun hello(): JSONObject = JSONObject()
        .put("ok", true)
        .put("protocol", PROTOCOL)
        .put("appVersionCode", versionCode())
        .put("remoteEnabled", remoteEnabled())

    private fun status(reply: Messenger, reqWhat: Int, args: Bundle) {
        val wantLogs = args.getInt("logs", 0).coerceIn(0, MAX_LOGS)
        val wantRefresh = args.getBoolean("refresh", false)
        if (!wantRefresh || controller.state.busy) {
            sendJson(reply, reqWhat, snapshot(null, wantLogs))
            return
        }
        // 外网连通性只有这一条路径会给：它真的发一次请求，所以只在要求刷新时跑。
        var reachable: Boolean? = null
        controller.refreshAndCheckInternet { ok -> reachable = ok }
        awaitSettled(STATUS_TIMEOUT_MS) { sendJson(reply, reqWhat, snapshot(reachable, wantLogs)) }
    }

    private fun login(reply: Messenger, reqWhat: Int) {
        if (!controller.isReady) {
            sendError(reply, reqWhat, "not_ready")
            return
        }
        if (controller.state.busy) {
            sendError(reply, reqWhat, "busy")
            return
        }
        val account = controller.account
        if (account.username.isBlank() || account.password.isEmpty()) {
            sendError(reply, reqWhat, "not_configured")
            return
        }
        controller.login(account)
        awaitSettled(LOGIN_TIMEOUT_MS) { sendJson(reply, reqWhat, snapshot(null, 0)) }
    }

    private fun logout(reply: Messenger, reqWhat: Int) {
        if (!controller.isReady) {
            sendError(reply, reqWhat, "not_ready")
            return
        }
        if (controller.state.busy) {
            sendError(reply, reqWhat, "busy")
            return
        }
        controller.logout(controller.account)
        awaitSettled(LOGOUT_TIMEOUT_MS) { sendJson(reply, reqWhat, snapshot(null, 0)) }
    }

    // ── 状态快照 ────────────────────────────────────────────────────────────

    private fun snapshot(reachable: Boolean?, wantLogs: Int): JSONObject {
        val state = controller.state
        val online = state.online
        val ssid = currentWifiSsid(applicationContext)
        return JSONObject().apply {
            put("ok", true)
            put("status", state.status)
            put("online", online != null)
            put("busy", state.busy)
            put("campus", when (state.campus) {
                CampusState.OnCampus -> "on_campus"
                CampusState.OffCampus -> "off_campus"
                else -> "unknown"
            })
            put("ssid", ssid ?: JSONObject.NULL)
            put("account", controller.account.fullUsername.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            put("ip", online?.ip ?: JSONObject.NULL)
            put("product", online?.product ?: JSONObject.NULL)
            put("usedSeconds", online?.usedSeconds ?: 0L)
            put("usedBytes", online?.usedBytes ?: 0.0)
            put("balance", online?.balance ?: 0.0)
            put("devices", online?.deviceTotal ?: 0)
            put("diag", JSONObject().apply {
                put("remoteEnabled", remoteEnabled())
                put("appVersionCode", versionCode())
                put("canReadSsid", canReadSsid(applicationContext))
                put("excluded", ssid != null && ssid in controller.excludedSsids)
                put("keepAlive", controller.keepAlive)
                put("internetReachable", reachable ?: JSONObject.NULL)
            })
            if (wantLogs > 0) {
                val arr = JSONArray()
                // HTTP 级别的行可能带认证参数，不往外递。
                controller.logs.filter { it.level != "HTTP" }.takeLast(wantLogs).forEach { entry ->
                    arr.put(JSONObject().put("level", entry.level).put("message", entry.message))
                }
                put("logs", arr)
            }
        }
    }

    // ── 工具 ────────────────────────────────────────────────────────────────

    /**
     * 等 `busy` 落回 false。
     *
     * 不挂 `controller.listener`：那个字段被 MainActivity 占着，抢过来界面就不刷新了。
     * 轮询比回调笨一点，但完全不碰别人的状态。
     */
    private fun awaitSettled(timeoutMs: Long, done: () -> Unit) {
        val start = System.currentTimeMillis()
        val poll = object : Runnable {
            var ticks = 0
            override fun run() {
                ticks++
                // 至少给它一个 tick 进入 busy，否则刚发起就判成结束
                if ((ticks > 1 && !controller.state.busy) || System.currentTimeMillis() - start > timeoutMs) {
                    done()
                } else {
                    handler.postDelayed(this, 120)
                }
            }
        }
        handler.postDelayed(poll, 150)
    }

    private fun remoteEnabled(): Boolean = settings.load().remoteEnabled

    private fun versionCode(): Int {
        val info = packageManager.getPackageInfo(packageName, 0)
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else info.versionCode
    }

    private fun callerAllowed(uid: Int): Boolean {
        if (uid == Process.myUid()) return true
        val packages = packageManager.getPackagesForUid(uid) ?: return false
        return packages.any { it in ALLOWED_PACKAGES }
    }

    private fun sendJson(reply: Messenger, reqWhat: Int, json: JSONObject) {
        val msg = Message.obtain(null, MSG_RESULT).apply {
            arg1 = reqWhat
            data = Bundle().apply { putString("json", json.toString()) }
        }
        runCatching { reply.send(msg) }
    }

    private fun sendError(reply: Messenger, reqWhat: Int, error: String) {
        sendJson(reply, reqWhat, JSONObject().put("ok", false).put("error", error))
    }

    companion object {
        /** 协议版本：加字段不改版本号，改语义才加。 */
        const val PROTOCOL = 1

        /** 服务类名，插件那边用显式组件绑。 */
        const val SERVICE_CLASS = "com.hz6826.campusnetlogin.remote.RemoteControlService"

        const val MSG_HELLO = 1
        const val MSG_STATUS = 2
        const val MSG_LOGIN = 3
        const val MSG_LOGOUT = 4
        const val MSG_RESULT = 100

        private const val MAX_LOGS = 200
        private const val STATUS_TIMEOUT_MS = 12_000L
        private const val LOGIN_TIMEOUT_MS = 40_000L
        private const val LOGOUT_TIMEOUT_MS = 15_000L

        /** 只放行岱宗盒子（正式包与 debug 包）。 */
        private val ALLOWED_PACKAGES = setOf(
            "com.xjtu.toolbox",
            "com.xjtu.toolbox.debug",
        )
    }
}
