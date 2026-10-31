package com.hz6826.xjtunetlogin.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** 应用设置（外观 + 日志开关），持久化在 appearance 偏好文件里。 */
data class AppSettings(
    val darkMode: String = "system",
    val dynamicColor: Boolean = false,
    val seedColor: Int? = null,
    val glassBar: Boolean = false,
    val logHttp: Boolean = false,
    val keepAlive: Boolean = false,
)

/** [seed] 为 null 表示使用 Miuix 原生默认蓝色配色（不是种子生成的）。 */
data class ThemeColorPreset(val name: String, val seed: Int?)

const val MIUIX_BLUE: Int = 0xFF3482FF.toInt()

/** 主题色预设：第一项是默认的 Miuix 原生蓝，其余是校园风命名。 */
val themeColorPresets: List<ThemeColorPreset> = listOf(
    ThemeColorPreset("Miuix 蓝（默认）", null),
    ThemeColorPreset("仙交蓝", 0xFF3482FF.toInt()),
    ThemeColorPreset("校园绿", 0xFF196653.toInt()),
    ThemeColorPreset("梧桐橙", 0xFFFFA726.toInt()),
    ThemeColorPreset("创新港青", 0xFF26A69A.toInt()),
    ThemeColorPreset("梧桐紫", 0xFF7E57C2.toInt()),
    ThemeColorPreset("交大红", 0xFFE94634.toInt()),
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val darkMode = prefs.getString("darkMode", "system") ?: "system"
        val migrated = !prefs.contains("dynamicColor") && !prefs.contains("seedColor")
        if (migrated) {
            // 没有旧 keyColor 的设置 = 新安装，默认用 Miuix 原生蓝。
            if (!prefs.contains("keyColor")) return AppSettings(darkMode = darkMode)
            // 旧版本只有 keyColor：0 表示「跟随系统取色」，非 0 是预设色。
            val oldKey = prefs.getInt("keyColor", 0)
            val dynamic = oldKey == 0
            return AppSettings(
                darkMode = darkMode,
                dynamicColor = dynamic,
                seedColor = if (dynamic) null else oldKey,
            )
        }
        return AppSettings(
            darkMode = darkMode,
            dynamicColor = prefs.getBoolean("dynamicColor", false),
            seedColor = prefs.getInt("seedColor", 0).takeIf { it != 0 },
            glassBar = prefs.getBoolean("glassBar", false),
            logHttp = prefs.getBoolean("logHttp", false),
            keepAlive = prefs.getBoolean("keepAlive", false),
        )
    }

    /** 旧版本 autoLogin 迁移到 keepAlive 只做一次，避免与开关状态互相覆盖。 */
    fun isKeepAliveMigrated(): Boolean = prefs.getBoolean("keepAliveMigrated", false)

    fun markKeepAliveMigrated() {
        prefs.edit().putBoolean("keepAliveMigrated", true).apply()
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("darkMode", settings.darkMode)
            .putBoolean("dynamicColor", settings.dynamicColor)
            .putInt("seedColor", settings.seedColor ?: 0)
            .putBoolean("glassBar", settings.glassBar)
            .putBoolean("logHttp", settings.logHttp)
            .putBoolean("keepAlive", settings.keepAlive)
            .apply()
    }
}

/**
 * 读取系统取色种子（Monet）。
 *
 * 国产 ROM 不一定支持动态取色：先尝试 Android 13+ 的系统调色板配置，再回退到 Android 12 的
 * system_accent1_500 资源；都拿不到就返回 null，由调用方回落到默认仙交蓝。
 */
fun resolveSystemSeedColor(context: Context): Color? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val raw = runCatching {
            Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages")
        }.getOrNull()
        if (!raw.isNullOrBlank()) {
            val hex = runCatching {
                JSONObject(raw).optString("android.theme.customization.system_palette")
            }.getOrNull()
            parseHexColor(hex)?.let { return it }
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        @Suppress("DEPRECATION")
        val argb = runCatching { context.resources.getColor(android.R.color.system_accent1_500, context.theme) }
            .getOrNull()
        if (argb != null && Color(argb).alpha > 0f) return Color(argb)
    }
    return null
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val normalized = if (hex.startsWith("#")) hex else "#$hex"
    return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrNull()
}
