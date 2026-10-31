package com.hz6826.xjtunetlogin

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hz6826.xjtunetlogin.data.AppSettings
import com.hz6826.xjtunetlogin.data.SettingsStore
import com.hz6826.xjtunetlogin.update.UpdateChecker
import com.hz6826.xjtunetlogin.update.UpdateRelease
import com.hz6826.xjtunetlogin.service.AutoLoginService
import com.hz6826.xjtunetlogin.session.SessionController
import com.hz6826.xjtunetlogin.session.SessionHolder
import com.hz6826.xjtunetlogin.ui.components.UpdateDialog
import com.hz6826.xjtunetlogin.ui.glass.GlassBottomTabs
import com.hz6826.xjtunetlogin.ui.glass.GlassTab
import com.hz6826.xjtunetlogin.ui.glass.LocalIsDarkTheme
import com.hz6826.xjtunetlogin.ui.nav.BottomBar
import com.hz6826.xjtunetlogin.ui.nav.LocalBottomBarInset
import com.hz6826.xjtunetlogin.ui.nav.bottomTabs
import com.hz6826.xjtunetlogin.ui.pages.AboutPage
import com.hz6826.xjtunetlogin.ui.pages.ConnectionPage
import com.hz6826.xjtunetlogin.ui.pages.ContributorsPage
import com.hz6826.xjtunetlogin.ui.pages.KeepAlivePage
import com.hz6826.xjtunetlogin.ui.pages.Draft
import com.hz6826.xjtunetlogin.ui.pages.LogPage
import com.hz6826.xjtunetlogin.ui.pages.LoginPage
import com.hz6826.xjtunetlogin.ui.pages.OnlineInfoPage
import com.hz6826.xjtunetlogin.ui.pages.ReferencesPage
import com.hz6826.xjtunetlogin.ui.pages.SettingsPage
import com.hz6826.xjtunetlogin.ui.pages.draftSaver
import com.hz6826.xjtunetlogin.ui.theme.AppTheme
import com.hz6826.xjtunetlogin.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = SessionHolder.get(applicationContext)
        setContent {
            val store = remember { SettingsStore(applicationContext) }
            var settings by rememberSaveable(stateSaver = settingsSaver) {
                mutableStateOf(store.load())
            }
            AppTheme(settings) {
                App(
                    controller = session,
                    settings = settings,
                    onSettingsChange = {
                        settings = it
                        store.save(it)
                    },
                    store = store,
                )
            }
        }
    }
}

private val settingsSaver = listSaver<AppSettings, Any>(
    save = {
        listOf(
            it.darkMode, it.dynamicColor, it.seedColor ?: 0, it.glassBar, it.logHttp, it.keepAlive,
            it.autoCheckUpdate, it.previewUpdates, it.ignoredUpdateVersion ?: "", it.lastUpdateCheck,
        )
    },
    restore = {
        AppSettings(
            darkMode = it[0] as String,
            dynamicColor = it[1] as Boolean,
            seedColor = (it[2] as Int).takeIf { c -> c != 0 },
            glassBar = it[3] as Boolean,
            logHttp = it[4] as Boolean,
            keepAlive = it[5] as Boolean,
            autoCheckUpdate = it[6] as Boolean,
            previewUpdates = it[7] as Boolean,
            ignoredUpdateVersion = (it[8] as String).takeIf { v -> v.isNotEmpty() },
            lastUpdateCheck = it[9] as Long,
        )
    },
)

@Composable
private fun App(
    controller: SessionController,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    store: SettingsStore,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(controller.state) }
    val logsState = remember { mutableStateOf(controller.logs) }
    var draft by rememberSaveable(stateSaver = draftSaver) { mutableStateOf(Draft()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var secondaryPage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateRelease by remember { mutableStateOf<UpdateRelease?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun applyLoaded() {
        if (!draft.loaded) {
            draft = draft.copy(
                username = controller.account.username,
                password = controller.account.password,
                domain = controller.account.domain,
                acId = controller.account.acId,
                baseUrl = controller.account.baseUrl,
                remember = controller.account.remember,
                loaded = true,
            )
        }
        state = controller.state
        logsState.value = controller.logs
    }

    fun checkForUpdate(auto: Boolean) {
        scope.launch {
            controller.recordLog("STEP", "→ 检查新版本")
            val result = runCatching {
                withContext(Dispatchers.IO) { UpdateChecker.check(context, settings.previewUpdates) }
            }
            result.onSuccess { release ->
                if (release == null) {
                    controller.recordLog("INFO", "已是最新版本")
                    if (!auto) Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                } else {
                    controller.recordLog("SUCCESS", "发现新版本: ${release.tag}")
                    if (!auto || settings.ignoredUpdateVersion != release.tag) updateRelease = release
                }
            }.onFailure {
                controller.recordLog("WARN", "版本检查失败: ${it.message.orEmpty()}")
                if (!auto) {
                    Toast.makeText(context, "版本检查失败：${it.message.orEmpty()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun installUpdate(release: UpdateRelease) {
        updateBusy = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { UpdateChecker.download(context, release) }
            }
            updateBusy = false
            result.onSuccess { file ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    Toast.makeText(context, "请允许「安装未知应用」后重试", Toast.LENGTH_LONG).show()
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                }
            }.onFailure {
                Toast.makeText(context, "更新失败：${it.message.orEmpty()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 启动时自动检查更新：默认开启，同一天内不重复请求，避免打满匿名 API 限额。
    LaunchedEffect(settings.autoCheckUpdate) {
        if (!settings.autoCheckUpdate) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < 24L * 60 * 60 * 1000) return@LaunchedEffect
        onSettingsChange(settings.copy(lastUpdateCheck = now))
        checkForUpdate(auto = true)
    }

    SideEffect {
        controller.setLogHttp(settings.logHttp)
        controller.setKeepAlive(settings.keepAlive)
    }

    // 只有登录成功过并记住了账户时，保活/自动登录才可用。
    val keepAliveAvailable = controller.canKeepAlive()

    // 旧版本账户里单独存过 autoLogin：只迁移一次，之后完全以开关为准，避免开关被弹回。
    LaunchedEffect(controller.account) {
        if (controller.account.autoLogin && controller.account.remember && !store.isKeepAliveMigrated()) {
            store.markKeepAliveMigrated()
            if (controller.canKeepAlive() && !settings.keepAlive) {
                onSettingsChange(settings.copy(keepAlive = true))
            }
        }
    }

    // 前台保活服务：开启后常驻通知、自动登录，并随开机自启恢复。
    // 只在「从开变关」时 stopService，避免冷启动时把系统正在恢复的服务一起停掉导致 FGS 超时崩溃。
    val serviceWanted = settings.keepAlive && keepAliveAvailable
    val serviceWasWanted = remember { mutableStateOf(false) }
    LaunchedEffect(serviceWanted) {
        val intent = Intent(context, AutoLoginService::class.java)
        if (serviceWanted) {
            serviceWasWanted.value = true
            context.startForegroundService(intent)
        } else if (serviceWasWanted.value) {
            serviceWasWanted.value = false
            context.stopService(intent)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        fun bind() {
            controller.listener = { newState, newLogs ->
                state = newState
                logsState.value = newLogs
            }
            controller.loaded = { applyLoaded() }
            controller.toastListener = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            if (controller.isReady) applyLoaded()
            controller.start()
        }
        fun unbind() {
            controller.listener = null
            controller.loaded = null
            controller.toastListener = null
            controller.stop()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> bind()
                Lifecycle.Event.ON_STOP -> unbind()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 首次组合发生在 ON_START 之后时观察者收不到该事件，这里补一次绑定。
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) bind()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unbind()
        }
    }

    fun goBack() {
        // 嵌套二级页（贡献者/参考项目）返回到「关于」，其余返回主界面。
        secondaryPage = if (secondaryPage == "contributors" || secondaryPage == "references") "about" else null
    }
    BackHandler(enabled = secondaryPage != null) { goBack() }

    val dark = isDarkTheme(settings)
    val glass = settings.glassBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val backdrop = rememberLayerBackdrop()
    val pageTitles = listOf("登录状态", "运行记录", "在线信息", "设置")

    // 二级页切换时两层内容会同时平移，缝隙里必须有不透明底色，否则会闪出窗口白底。
    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
    AnimatedContent(
        targetState = secondaryPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            // 用页面深度判断方向：0 = 主界面，1 = 一级二级页，2 = 嵌套二级页（贡献者/参考项目）。
            fun depth(page: String?): Int = when (page) {
                null -> 0
                "contributors", "references" -> 2
                else -> 1
            }
            if (depth(targetState) > depth(initialState)) {
                // 进入更深一层：从右侧划入，旧页轻微左移淡出。
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
            } else {
                // 返回上一层：当前页向右划出，上一层从左侧滑回。
                (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "secondaryPage",
    ) { page ->
    if (page != null) {
        val titles = mapOf(
            "about" to "关于",
            "connection" to "连接设置",
            "keepalive" to "保活配置",
            "contributors" to "贡献者",
            "references" to "参考项目及许可",
        )
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = titles[page] ?: "",
                    navigationIcon = {
                        IconButton(onClick = { goBack() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (page) {
                    "connection" -> ConnectionPage(
                        draft = draft,
                        onDraftChange = { draft = it },
                        locked = state.busy || state.online != null,
                        lockedReason = when {
                            state.online != null -> "已连接校园网，连接设置已锁定；注销后可修改。"
                            state.busy -> "正在处理请求，请稍候…"
                            else -> null
                        },
                        onOpenWifi = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                        onSuggestWifi = { suggestCampusWifi(context) },
                    )
                    "keepalive" -> KeepAlivePage(settings = settings, onSettingsChange = onSettingsChange, available = keepAliveAvailable)
                    "contributors" -> ContributorsPage()
                    "references" -> ReferencesPage()
                    else -> AboutPage(
                        onOpenContributors = { secondaryPage = "contributors" },
                        onOpenReferences = { secondaryPage = "references" },
                    )
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize().then(if (glass) Modifier.layerBackdrop(backdrop) else Modifier),
                topBar = {
                    SmallTopAppBar(
                        title = pageTitles[selectedTab],
                    )
                },
                bottomBar = {
                    if (!glass) BottomBar(selected = selectedTab, onSelect = { selectedTab = it })
                },
            ) { innerPadding ->
                CompositionLocalProvider(LocalBottomBarInset provides if (glass) 96.dp else 0.dp) {
                    AnimatedContent(
                        targetState = selectedTab,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "tabContent",
                    ) { tab ->
                        when (tab) {
                            0 -> LoginPage(controller, state, draft, onDraftChange = { draft = it })
                            1 -> LogPage(
                                controller = controller,
                                logsState = logsState,
                                logHttp = settings.logHttp,
                                onLogHttpChange = { onSettingsChange(settings.copy(logHttp = it)) },
                            )
                            2 -> OnlineInfoPage(controller, state)
                            else -> SettingsPage(
                                settings = settings,
                                onSettingsChange = onSettingsChange,
                                onOpenConnection = { secondaryPage = "connection" },
                                onOpenKeepAlive = { secondaryPage = "keepalive" },
                                onOpenAbout = { secondaryPage = "about" },
                                onCheckUpdate = { checkForUpdate(auto = false) },
                            )
                        }
                    }
                }
            }
            updateRelease?.let { release ->
                UpdateDialog(
                    release = release,
                    busy = updateBusy,
                    onInstall = { installUpdate(release) },
                    onLater = { updateRelease = null },
                    onIgnore = {
                        onSettingsChange(settings.copy(ignoredUpdateVersion = release.tag))
                        updateRelease = null
                    },
                )
            }
            if (glass) {
                val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                CompositionLocalProvider(LocalIsDarkTheme provides dark) {
                    GlassBottomTabs(
                        tabs = bottomTabs.map { tab ->
                            GlassTab(key = tab.label) { selected ->
                                val tint = if (selected) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                }
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp),
                                    tint = tint,
                                )
                                Text(tab.label, fontSize = 12.sp, maxLines = 1, color = tint)
                            }
                        },
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        backdrop = backdrop,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .systemGestureExclusion()
                            .padding(horizontal = 40.dp)
                            .padding(bottom = navInset + if (navInset > 0.dp) 20.dp else 32.dp),
                    )
                }
            }
        }
    }
    }
    }
}

private fun suggestCampusWifi(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val suggestion = WifiNetworkSuggestion.Builder().setSsid("XJTU-STU").build()
    val result = try {
        wifi.addNetworkSuggestions(listOf(suggestion))
    } catch (_: SecurityException) {
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED
    }
    val message = when (result) {
        WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS,
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE ->
            "已提交 XJTU-STU 连接建议，请按系统提示允许。连接由系统选择。"
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED ->
            "系统未允许 Wi-Fi 控制，请在应用的特殊访问权限中开启。"
        else -> "添加 Wi-Fi 连接建议失败（$result），请检查系统 Wi-Fi 设置。"
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
