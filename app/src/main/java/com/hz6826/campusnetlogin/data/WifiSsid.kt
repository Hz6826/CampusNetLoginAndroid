package com.hz6826.campusnetlogin.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

private const val TAG = "WifiSsid"
private const val UNKNOWN_SSID = "<unknown ssid>"

/** 应用内运行日志出口；由 SessionController 注入，data 层不依赖 session 层。 */
@Volatile
var ssidLogSink: ((level: String, message: String) -> Unit)? = null

// 轮询时同一个诊断信息会反复出现，只记第一次，避免把应用内日志刷满。
@Volatile
private var lastReported: String? = null

/**
 * 现在能否读到 SSID：ACCESS_WIFI_STATE（安装即授予）+ Android 8.1 起的定位权限 + 系统定位服务开启。
 * 注意名字是「能不能读」，不是单纯的权限判断；定位服务关闭时同样返回 false。
 */
fun canReadSsid(context: Context): Boolean {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) != PackageManager.PERMISSION_GRANTED
    ) return false

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return true

    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocation) return false

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager?.isLocationEnabled == true
    } else {
        @Suppress("DEPRECATION")
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    return isLocationEnabled
}

/** 当前 Wi-Fi 名称；无权限或无法读取时返回 null。 */
fun currentWifiSsid(context: Context): String? {
    if (!canReadSsid(context)) {
        report("INFO", "缺少定位权限或定位服务未开启，跳过读取 Wi-Fi 名称")
        return null
    }

    return try {
        val manager = context.applicationContext
            .getSystemService(WifiManager::class.java) ?: return null

        @Suppress("DEPRECATION")
        val raw = manager.connectionInfo?.ssid ?: return null

        val ssid = normalizeSsid(raw).takeUnless {
            it.isBlank() || it == UNKNOWN_SSID
        }
        if (ssid != null) lastReported = null
        ssid
    } catch (e: SecurityException) {
        report("WARN", "读取 Wi-Fi 名称权限不足", e)
        null
    } catch (e: Exception) {
        report("WARN", "读取 Wi-Fi 名称失败", e)
        null
    }
}

/** 同时写 logcat 和应用内运行日志；相同信息连续出现时只记一次。 */
private fun report(level: String, message: String, error: Throwable? = null) {
    if (message == lastReported) return
    lastReported = message
    when (level) {
        "WARN" -> Log.w(TAG, message, error)
        "ERROR" -> Log.e(TAG, message, error)
        else -> Log.d(TAG, message, error)
    }
    ssidLogSink?.invoke(level, message)
}

fun normalizeSsid(raw: String): String = raw.trim().removePrefix("\"").removeSuffix("\"")
