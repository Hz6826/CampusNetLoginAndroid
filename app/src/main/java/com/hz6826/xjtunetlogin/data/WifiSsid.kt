package com.hz6826.xjtunetlogin.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Android 8.1 起读取 SSID 需要定位权限。 */
fun hasSsidPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

/** 当前 Wi-Fi 名称；无权限或无法读取时返回 null。 */
fun currentWifiSsid(context: Context): String? {
    if (!hasSsidPermission(context)) return null
    val manager = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
    @Suppress("DEPRECATION")
    val raw = manager.connectionInfo?.ssid ?: return null
    return normalizeSsid(raw).takeUnless {
        it.isBlank() || it == "<unknown ssid>" || it == WifiManager.UNKNOWN_SSID
    }
}

fun normalizeSsid(raw: String): String = raw.trim().removePrefix("\"").removeSuffix("\"")
