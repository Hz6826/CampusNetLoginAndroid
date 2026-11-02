package com.hz6826.campusnetlogin.model

import org.json.JSONObject
import java.util.Locale

/** 在线信息快照；只保留界面需要的值，避免把 JSONObject 传进 Compose（不稳定类型会拖累重组跳过）。 */
data class OnlineInfo(
    val account: String,
    val ip: String,
    val mac: String,
    val product: String,
    val billing: String,
    val usedBytes: Double,
    val usedSeconds: Long,
    val balance: Double,
    val deviceTotal: Int,
) {
    companion object {
        fun from(json: JSONObject?): OnlineInfo? {
            if (json == null || json.optString("error") != "ok") return null
            val user = json.optString("user_name")
            val domain = json.optString("domain").removePrefix("@")
            val account = when {
                user.isEmpty() -> "-"
                domain.isEmpty() || '@' in user -> user
                else -> "$user@$domain"
            }
            return OnlineInfo(
                account = account,
                ip = json.optString("online_ip").ifBlank { "-" },
                mac = json.optString("user_mac").ifBlank { "-" },
                product = json.optString("products_name").ifBlank { "-" },
                billing = json.optString("billing_name").ifBlank { "-" },
                usedBytes = json.optDouble("sum_bytes", 0.0),
                usedSeconds = json.optLong("sum_seconds", 0),
                balance = json.optDouble("user_balance", 0.0),
                deviceTotal = json.optInt("online_device_total", 0),
            )
        }
    }
}

/** 与原项目 formatBytes 一致：<=0 显示 0 B，其余保留两位小数。 */
fun formatBytes(bytes: Double): String {
    if (bytes <= 0) return "0 B"
    var value = bytes
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.CHINA, "%.2f %s", value, units[index])
}

/** 与原项目 formatDuration 一致：天/小时/分，只有不足一分钟时才显示秒。 */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0 秒"
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val parts = mutableListOf<String>()
    if (d > 0) parts += "$d 天"
    if (h > 0 || d > 0) parts += "$h 小时"
    if (m > 0 || h > 0 || d > 0) parts += "$m 分"
    if (s > 0 && parts.isEmpty()) parts += "$s 秒"
    if (parts.isEmpty()) parts += "0 秒"
    return parts.joinToString(" ")
}
