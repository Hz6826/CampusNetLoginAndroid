package com.hz6826.campusnetlogin.ui.pages

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hz6826.campusnetlogin.data.AppSettings
import com.hz6826.campusnetlogin.ui.components.SettingsCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 「保活配置」二级页：前台常驻通知开关 + 两个省电策略入口。 */
@Composable
fun KeepAlivePage(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    available: Boolean,
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    // 没登录成功过或没记住账户时不可开启（与服务的真实运行条件保持一致）。
    val switchEnabled = available

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) {
        SettingsCard {
            SwitchPreference(
                title = "前台保活与自动登录",
                summary = if (available) {
                    "常驻通知显示校园网状态、账号与已用时长，并自动登录校园网；开启后同时支持开机自启"
                } else {
                    "需要先在登录页登录成功并勾选「记住账户」后才能开启"
                },
                checked = settings.keepAlive && available,
                enabled = switchEnabled,
                onCheckedChange = { enabled ->
                    onSettingsChange(settings.copy(keepAlive = enabled))
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsCard {
            ArrowPreference(
                title = "忽略电池优化",
                summary = "把本应用加入系统电池优化白名单，减少后台被清理",
                onClick = { openBatteryOptimization(context) },
            )
            ArrowPreference(
                title = "应用电池设置",
                summary = "在系统设置里允许后台运行 / 自启动 / 无限制耗电",
                onClick = { openAppSettings(context) },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "开启后会常驻通知、自动登录，并随开机自启恢复；关闭后不再常驻、也不再自动登录。不同厂商的省电策略入口不完全一样，如果通知被清理或开机后没有恢复，请把本应用设为「无限制」并在系统里允许自启动。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

private fun openBatteryOptimization(context: Context) {
    val request = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    )
    runCatching { context.startActivity(request) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )
    runCatching { context.startActivity(intent) }
}
