package com.hz6826.campusnetlogin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hz6826.campusnetlogin.data.AppSettings
import com.hz6826.campusnetlogin.data.SettingsStore
import com.hz6826.campusnetlogin.data.canReadSsid
import com.hz6826.campusnetlogin.data.currentWifiSsid
import com.hz6826.campusnetlogin.data.normalizeSsid
import com.hz6826.campusnetlogin.update.UpdateChecker
import com.hz6826.campusnetlogin.update.UpdateRelease
import com.hz6826.campusnetlogin.service.AutoLoginService
import com.hz6826.campusnetlogin.session.SessionController
import com.hz6826.campusnetlogin.session.SessionHolder
import com.hz6826.campusnetlogin.ui.components.UpdateDialog
import com.hz6826.campusnetlogin.ui.pages.ExcludedSsidsPage
import com.hz6826.campusnetlogin.ui.glass.GlassBottomTabs
import com.hz6826.campusnetlogin.ui.glass.GlassTab
import com.hz6826.campusnetlogin.ui.glass.LocalIsDarkTheme
import com.hz6826.campusnetlogin.ui.nav.BottomBar
import com.hz6826.campusnetlogin.ui.nav.LocalBottomBarInset
import com.hz6826.campusnetlogin.ui.nav.bottomTabs
import com.hz6826.campusnetlogin.ui.pages.AboutPage
import com.hz6826.campusnetlogin.ui.pages.ConnectionPage
import com.hz6826.campusnetlogin.ui.pages.ContributorsPage
import com.hz6826.campusnetlogin.ui.pages.KeepAlivePage
import com.hz6826.campusnetlogin.ui.pages.Draft
import com.hz6826.campusnetlogin.ui.pages.LogPage
import com.hz6826.campusnetlogin.ui.pages.LoginPage
import com.hz6826.campusnetlogin.ui.pages.OnlineInfoPage
import com.hz6826.campusnetlogin.ui.pages.ReferencesPage
import com.hz6826.campusnetlogin.ui.pages.SettingsPage
import com.hz6826.campusnetlogin.ui.pages.draftSaver
import com.hz6826.campusnetlogin.ui.theme.AppTheme
import com.hz6826.campusnetlogin.ui.theme.isDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.core.net.toUri

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
            it.excludedSsids.toList(),
            it.campusSsid,
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
            excludedSsids = (it[10] as List<*>).filterIsInstance<String>().toSet(),
            campusSsid = it[11] as String,
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
    var updateSheetVisible by remember { mutableStateOf(false) }
    var updateBusy by remember { mutableStateOf(false) }
    var showAddExcluded by remember { mutableStateOf(false) }
    var showClearExcluded by remember { mutableStateOf(false) }
    var newExcludedSsid by remember { mutableStateOf("") }
    var excludeSheetVisible by remember { mutableStateOf(false) }
    var excludeSheetSsid by remember { mutableStateOf<String?>(null) }
    // 只有点了状态卡上的「加入排除列表」链接才继续；启动时的静默授权不弹任何窗。
    var excludeRequestedByUser by remember { mutableStateOf(false) }
    // 等待系统授权弹窗时盖在应用页面之上的说明页。
    var permissionPageVisible by remember { mutableStateOf(false) }
    // 授权请求是否还在等系统回调；说明页延迟到确认授权窗真的弹出后再显示。
    var permissionRequestActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ssidPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequestActive = false
        permissionPageVisible = false
        if (excludeRequestedByUser) {
            excludeRequestedByUser = false
            if (permissions.values.all { it }) {
                val ssid = currentWifiSsid(context)
                if (ssid != null) {
                    excludeSheetSsid = ssid
                    excludeSheetVisible = true
                } else {
                    Toast.makeText(context, "无法获取 Wi-Fi 名称，请检查系统定位服务（GPS）是否已开启", Toast.LENGTH_LONG).show()
                }
            } else {
                val activity = context as? android.app.Activity
                if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    Toast.makeText(context, "请在系统设置中授予定位权限", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "需要定位权限才能读取当前 Wi-Fi 名称", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 真正缺失、会弹系统授权窗的权限；定位服务没开时这里为空，不该弹说明页。
    fun missingSsidPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return missing
    }

    // 先发起系统授权，再延迟显示说明页：永久拒绝时系统会立即回调，页面就不会闪一下。
    fun requestSsidPermissions(missing: List<String>) {
        permissionRequestActive = true
        ssidPermissionLauncher.launch(missing.toTypedArray())
        scope.launch {
            delay(200)
            if (permissionRequestActive) permissionPageVisible = true
        }
    }

    fun requestExcludeCurrentNetwork() {
        if (!canReadSsid(context)) {
            val missing = missingSsidPermissions()
            if (missing.isEmpty()) {
                // 权限都在，只是系统定位服务没开。
                Toast.makeText(context, "请开启系统定位服务后再读取 Wi-Fi 名称", Toast.LENGTH_LONG).show()
            } else {
                excludeRequestedByUser = true
                requestSsidPermissions(missing)
            }
        } else {
            val ssid = currentWifiSsid(context)
            if (ssid != null) {
                excludeSheetSsid = ssid
                excludeSheetVisible = true
            } else {
                Toast.makeText(context, "无法获取 Wi-Fi 名称，请检查系统定位服务（GPS）是否已开启", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                    if (!auto || settings.ignoredUpdateVersion != release.tag) {
                        updateRelease = release
                        updateSheetVisible = true
                    }
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
                if (!context.packageManager.canRequestPackageInstalls()
                ) {
                    Toast.makeText(context, "请允许「安装未知应用」后重试", Toast.LENGTH_LONG).show()
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            "package:${context.packageName}".toUri(),
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

    // 首次打开 App 时，如果没有权限，先发起申请，说明页随后显示在授权窗下面。
    LaunchedEffect(Unit) {
        if (!canReadSsid(context)) {
            val missing = missingSsidPermissions()
            if (missing.isNotEmpty()) {
                excludeRequestedByUser = false
                requestSsidPermissions(missing)
            }
        }
    }

    SideEffect {
        controller.setLogHttp(settings.logHttp)
        controller.setKeepAlive(settings.keepAlive)
        controller.setExcludedSsids(settings.excludedSsids)
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
    val pageTitles = listOf("校园网络助手", "运行日志", "在线信息", "设置")

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
            "keepalive" to "保活设置",
            "excluded" to "排除列表",
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
                    actions = {
                        if (page == "excluded") {
                            IconButton(
                                onClick = { if (settings.excludedSsids.isNotEmpty()) showClearExcluded = true },
                                enabled = settings.excludedSsids.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = "清除排除列表",
                                    modifier = Modifier.size(22.dp),
                                    tint = MiuixTheme.colorScheme.error,
                                )
                            }
                            IconButton(onClick = { newExcludedSsid = ""; showAddExcluded = true }) {
                                Icon(
                                    imageVector = MiuixIcons.Add,
                                    contentDescription = "添加排除的 Wi-Fi",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
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
                        onSuggestWifi = {
                            suggestCampusWifi(
                                context,
                                settings.campusSsid.trim().ifBlank { currentWifiSsid(context).orEmpty() },
                            )
                        },
                        campusSsid = settings.campusSsid,
                        onCampusSsidChange = { onSettingsChange(settings.copy(campusSsid = it)) },
                    )
                    "keepalive" -> KeepAlivePage(settings = settings, onSettingsChange = onSettingsChange, available = keepAliveAvailable)
                    "excluded" -> ExcludedSsidsPage(
                        excluded = settings.excludedSsids.sorted(),
                        onRemove = { ssid ->
                            onSettingsChange(settings.copy(excludedSsids = settings.excludedSsids - ssid))
                        },
                        onRestore = { ssid ->
                            onSettingsChange(settings.copy(excludedSsids = settings.excludedSsids + ssid))
                        },
                    )
                    "contributors" -> ContributorsPage()
                    "references" -> ReferencesPage()
                    else -> AboutPage(
                        onOpenContributors = { secondaryPage = "contributors" },
                        onOpenReferences = { secondaryPage = "references" },
                    )
                }
            }
        }
        if (page == "excluded") {
            WindowDialog(
                show = showAddExcluded,
                title = "添加排除的 Wi-Fi",
                summary = "输入 Wi-Fi 名称（SSID），该网络不会被视为校园网，也不会自动登录。",
                onDismissRequest = { showAddExcluded = false; newExcludedSsid = "" },
                onDismissFinished = { newExcludedSsid = "" },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = newExcludedSsid,
                        onValueChange = { newExcludedSsid = it },
                        label = "Wi-Fi 名称",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            text = "取消",
                            onClick = { showAddExcluded = false; newExcludedSsid = "" },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "添加",
                            onClick = {
                                val ssid = normalizeSsid(newExcludedSsid)
                                if (ssid.isNotBlank()) {
                                    onSettingsChange(settings.copy(excludedSsids = settings.excludedSsids + ssid))
                                }
                                showAddExcluded = false
                                newExcludedSsid = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
            WindowDialog(
                show = showClearExcluded,
                title = "清除排除列表？",
                summary = "将移除全部已排除的 Wi-Fi，清除后这些网络会重新被当作校园网检测。",
                onDismissRequest = { showClearExcluded = false },
                onDismissFinished = {},
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showClearExcluded = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        text = "清除",
                        onClick = {
                            onSettingsChange(settings.copy(excludedSsids = emptySet()))
                            showClearExcluded = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
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
                            0 -> LoginPage(
                                controller = controller,
                                state = state,
                                draft = draft,
                                onOpenKeepAlive = { selectedTab = 0; secondaryPage = "keepalive" },
                                onOpenConnection = { selectedTab = 0; secondaryPage = "connection" },
                                onRequestExclude = { requestExcludeCurrentNetwork() },
                                onDraftChange = { draft = it },
                            )
                            1 -> LogPage(
                                controller = controller,
                                logsState = logsState,
                                logHttp = settings.logHttp,
                                onLogHttpChange = { onSettingsChange(settings.copy(logHttp = it)) },
                            )
                            2 -> OnlineInfoPage(controller, state, onRequestExclude = { requestExcludeCurrentNetwork() })
                            else -> SettingsPage(
                                settings = settings,
                                onSettingsChange = onSettingsChange,
                                onOpenConnection = { secondaryPage = "connection" },
                                onOpenKeepAlive = { secondaryPage = "keepalive" },
                                onOpenExcluded = { secondaryPage = "excluded" },
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
                    show = updateSheetVisible,
                    busy = updateBusy,
                    onDismissRequest = { if (!updateBusy) updateSheetVisible = false },
                    onDismissFinished = { if (!updateSheetVisible) updateRelease = null },
                    onInstall = { installUpdate(release) },
                    onIgnore = {
                        onSettingsChange(settings.copy(ignoredUpdateVersion = release.tag))
                        updateSheetVisible = false
                    },
                )
            }
            WindowBottomSheet(
                show = excludeSheetVisible,
                title = "将${excludeSheetSsid ?: "当前 Wi-Fi"}加入排除列表？",
                onDismissRequest = { excludeSheetVisible = false },
                onDismissFinished = { excludeSheetVisible = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "宿舍网等网络有时也能访问到内网认证服务器，如果你确定这不是校园网，可点击确认将该 Wi-Fi 加入排除列表。",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            text = "取消",
                            onClick = { excludeSheetVisible = false },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "确认",
                            onClick = {
                                val ssid = excludeSheetSsid
                                if (ssid != null) {
                                    onSettingsChange(settings.copy(excludedSsids = settings.excludedSsids + ssid))
                                    Toast.makeText(context, "已将 $ssid 加入排除列表", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "无法读取当前 Wi-Fi 名称，请到「设置 → 连接 → 排除列表」手动添加",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                                excludeSheetVisible = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
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
    PermissionNoticePage(visible = permissionPageVisible)
    }
}

/**
 * 等待系统授权弹窗时的权限说明页：盖在应用页面之上、系统授权窗之下。
 * 说明文字放在屏幕上方，避免被大多数机型的授权弹窗（底部/居中）遮住。
 */
@Composable
private fun PermissionNoticePage(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 12 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 12 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                // 授权窗正在关闭的短暂动画期间也挡住误触。
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 28.dp)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "读取 Wi-Fi 名称\n需要授权",
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "为了识别当前连接的是哪个校园 Wi-Fi，需要「定位」权限。",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "该权限仅用于读取 Wi-Fi 名称（SSID），不会获取、保存或上传你的位置及其他个人隐私信息。",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "请在系统弹窗中选择「允许」",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun suggestCampusWifi(context: Context, ssid: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    if (ssid.isBlank()) {
        Toast.makeText(context, "请先连接校园 Wi-Fi，或填写校园 Wi-Fi 名称", Toast.LENGTH_LONG).show()
        return
    }
    // SSID 上限 32 字节，超长时 setSsid 会抛 IllegalArgumentException
    if (ssid.toByteArray(Charsets.UTF_8).size > 32) {
        Toast.makeText(context, "校园 Wi-Fi 名称不能超过 32 字节", Toast.LENGTH_LONG).show()
        return
    }
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val suggestion = WifiNetworkSuggestion.Builder().setSsid(ssid).build()
    val result = try {
        wifi.addNetworkSuggestions(listOf(suggestion))
    } catch (_: SecurityException) {
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED
    }
    val message = when (result) {
        WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS,
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE ->
            "已提交 $ssid 连接建议，请按系统提示允许。连接由系统选择。"
        WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED ->
            "系统未允许 Wi-Fi 控制，请在应用的特殊访问权限中开启。"
        else -> "添加 Wi-Fi 连接建议失败（$result），请检查系统 Wi-Fi 设置。"
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
