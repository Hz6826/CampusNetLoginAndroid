package com.hz6826.xjtunetlogin.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.hz6826.xjtunetlogin.data.AppSettings
import com.hz6826.xjtunetlogin.data.MIUIX_BLUE
import com.hz6826.xjtunetlogin.data.resolveSystemSeedColor
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 状态徽标语义色：等待连接 / 已连接（无需连接用主题的 onBackgroundVariant 灰）。 */
val StatusWaiting = Color(0xFF3482FF)
val StatusOnline = Color(0xFF4EC96B)

@Composable
fun isDarkTheme(settings: AppSettings): Boolean = when (settings.darkMode) {
    "light" -> false
    "dark" -> true
    else -> isSystemInDarkTheme()
}

/**
 * 应用主题。
 *
 * 统一用种子色生成 Miuix 配色（materialkolor 纯计算，不依赖系统动态取色能力）：
 * - 「跟随系统取色」时尝试读系统种子，读不到（国产 ROM 常见）就回落仙交蓝；
 * - 预设色直接作为种子。
 * 生成后的配色按岱宗盒子的思路重排 surface/surfaceVariant，避免 MD3 角色把页面与卡片对调。
 */
@Composable
fun AppTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isDarkTheme(settings)
    val systemSeed = remember(settings.dynamicColor) {
        if (settings.dynamicColor) resolveSystemSeedColor(context) else null
    }
    // seedColor == null 且不跟随系统取色 = Miuix 原生默认蓝色。
    val useMiuixDefault = !settings.dynamicColor && settings.seedColor == null
    val seed = when {
        settings.dynamicColor -> systemSeed ?: Color(MIUIX_BLUE)
        settings.seedColor != null -> Color(settings.seedColor)
        else -> Color(MIUIX_BLUE)
    }
    val mode = when (settings.darkMode) {
        "light" -> ColorSchemeMode.MonetLight
        "dark" -> ColorSchemeMode.MonetDark
        else -> ColorSchemeMode.MonetSystem
    }
    // 关键：两个分支都走同一个 MiuixTheme(colors=...) 调用点，切换主题时内容子树不会被销毁，
    // 否则 rememberSaveable（当前 tab、二级页等）会被丢掉、界面跳回主页。
    val colors = if (useMiuixDefault) {
        if (dark) darkColorScheme() else lightColorScheme()
    } else {
        val controller = remember(mode, seed) {
            ThemeController(colorSchemeMode = mode, keyColor = seed)
        }
        remapMonetColorsToMiuix(controller.currentColors(), dark)
    }
    PlatformSystemBars(dark)
    MiuixTheme(colors = colors, content = content)
}

/** Monet / MD3 角色灌进 Miuix 后页面与卡片会反转，这里按 Miuix 的「灰页面 / 白卡片」重排。 */
private fun remapMonetColorsToMiuix(colors: Colors, dark: Boolean): Colors = if (dark) {
    colors.copy(surfaceVariant = colors.surfaceContainer)
} else {
    colors.copy(
        surface = colors.surfaceContainer,
        surfaceVariant = colors.surface,
        surfaceContainer = colors.surface,
    )
}

@Composable
private fun PlatformSystemBars(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { dark },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.window?.isNavigationBarContrastEnforced = false
        }
        onDispose {}
    }
}
