package com.hz6826.campusnetlogin.ui.pages

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.data.AppSettings
import com.hz6826.campusnetlogin.data.MIUIX_BLUE
import com.hz6826.campusnetlogin.data.resolveSystemSeedColor
import com.hz6826.campusnetlogin.data.themeColorPresets
import com.hz6826.campusnetlogin.ui.components.SettingsCard
import com.hz6826.campusnetlogin.ui.nav.LocalBottomBarInset
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenConnection: () -> Unit,
    onOpenKeepAlive: () -> Unit,
    onOpenExcluded: () -> Unit,
    onOpenAbout: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val darkModeValues = listOf("system", "light", "dark")
    val themeItems = listOf("跟随系统取色") + themeColorPresets.map { it.name }
    val themeIndex = if (settings.dynamicColor) {
        0
    } else {
        val index = themeColorPresets.indexOfFirst { it.seed == settings.seedColor }
        if (index >= 0) index + 1 else 1
    }
    val systemSeed = remember(settings.dynamicColor) {
        if (settings.dynamicColor) resolveSystemSeedColor(context) else null
    }
    val dotColor = when {
        settings.dynamicColor -> systemSeed ?: Color(MIUIX_BLUE)
        settings.seedColor != null -> Color(settings.seedColor)
        else -> Color(MIUIX_BLUE)
    }
    val versionName = remember {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }
    val glassSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SmallTitle("外观")
        SettingsCard {
            OverlayDropdownPreference(
                items = listOf("跟随系统", "浅色", "深色"),
                selectedIndex = darkModeValues.indexOf(settings.darkMode).coerceAtLeast(0),
                title = "深色模式",
                onSelectedIndexChange = { index ->
                    onSettingsChange(settings.copy(darkMode = darkModeValues[index]))
                },
            )
            OverlayDropdownPreference(
                items = themeItems,
                selectedIndex = themeIndex,
                title = "主题色",
                startAction = {
                    Box(Modifier.size(18.dp).background(dotColor, CircleShape))
                },
                onSelectedIndexChange = { index ->
                    if (index == 0) {
                        onSettingsChange(settings.copy(dynamicColor = true))
                    } else {
                        onSettingsChange(
                            settings.copy(dynamicColor = false, seedColor = themeColorPresets[index - 1].seed),
                        )
                    }
                },
            )
            SwitchPreference(
                title = "液态玻璃底栏",
                summary = if (glassSupported) "浮动胶囊 + 背景模糊（Apple 风格）" else "需要 Android 12 及以上",
                checked = settings.glassBar && glassSupported,
                enabled = glassSupported,
                onCheckedChange = { onSettingsChange(settings.copy(glassBar = it)) },
            )
        }

        SmallTitle("连接")
        SettingsCard {
            ArrowPreference(
                title = "保活设置",
                summary = "前台常驻通知、开机自启与省电策略",
                onClick = onOpenKeepAlive,
            )
            ArrowPreference(
                title = "连接设置",
                summary = "账户域、AC ID、认证地址与校园 Wi-Fi",
                onClick = onOpenConnection,
            )
            ArrowPreference(
                title = "排除列表",
                summary = if (settings.excludedSsids.isEmpty()) "不自动登录、也不当作校园网的 Wi-Fi" else "已排除 ${settings.excludedSsids.size} 个 Wi-Fi",
                onClick = onOpenExcluded,
            )
        }

        SmallTitle("关于")
        SettingsCard {
            ArrowPreference(
                title = "关于",
                summary = "版本 v$versionName",
                onClick = onOpenAbout,
            )
        }

        SmallTitle("更新")
        SettingsCard {
            SwitchPreference(
                title = "自动检查更新",
                summary = "启动应用时从 GitHub Releases 检查新版本",
                checked = settings.autoCheckUpdate,
                onCheckedChange = { onSettingsChange(settings.copy(autoCheckUpdate = it)) },
            )
            SwitchPreference(
                title = "接收预览版",
                summary = "检查时包含 beta 预发布版本",
                checked = settings.previewUpdates,
                onCheckedChange = { onSettingsChange(settings.copy(previewUpdates = it)) },
            )
            ArrowPreference(
                title = "立即检查更新",
                summary = "当前版本 v$versionName",
                onClick = onCheckUpdate,
            )
        }
        Spacer(Modifier.height(16.dp + LocalBottomBarInset.current))
    }
}
