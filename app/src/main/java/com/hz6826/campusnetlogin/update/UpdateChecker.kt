package com.hz6826.campusnetlogin.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** 一条可安装的 GitHub Release。 */
data class UpdateRelease(
    val tag: String,
    val title: String,
    val body: String,
    val htmlUrl: String,
    val prerelease: Boolean,
    val apkUrl: String,
    val sha256: String?,
)

/**
 * 基于 GitHub Releases 匿名 API 的更新检查（不需要 token）。
 * 只读取正式/预览 Release，按语义化版本比较；下载后用 API 给出的 sha256 校验。
 */
object UpdateChecker {
    private const val OWNER = "Hz6826"
    private const val REPO = "CampusNetLoginAndroid"
    private const val API = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30"
    private const val USER_AGENT = "CampusNetLoginAndroid"

    fun currentVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("0")

    /** 返回比当前版本更新的 Release；没有则 null。 */
    suspend fun check(context: Context, includePrerelease: Boolean): UpdateRelease? =
        withContext(Dispatchers.IO) {
            val body = httpGet(API)
            val releases = JSONArray(body)
            val current = currentVersion(context)
            var best: JSONObject? = null
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft")) continue
                if (release.optBoolean("prerelease") && !includePrerelease) continue
                val tag = release.optString("tag_name").trim()
                if (tag.isEmpty()) continue
                if (compareVersions(tag, current) <= 0) continue
                if (best == null || compareVersions(tag, best.optString("tag_name")) > 0) best = release
            }
            val release = best ?: return@withContext null
            val assets = release.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            var digest: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
                apkUrl = asset.optString("browser_download_url").ifBlank { continue }
                digest = asset.optString("digest")
                break
            }
            val url = apkUrl ?: return@withContext null
            UpdateRelease(
                tag = release.optString("tag_name"),
                title = release.optString("name").ifBlank { release.optString("tag_name") },
                body = release.optString("body"),
                htmlUrl = release.optString("html_url"),
                prerelease = release.optBoolean("prerelease"),
                apkUrl = url,
                sha256 = digest?.removePrefix("sha256:")?.takeIf { it.length == 64 },
            )
        }

    /** 下载并校验 APK；缺少校验值或校验失败会抛异常。 */
    suspend fun download(context: Context, release: UpdateRelease): File = withContext(Dispatchers.IO) {
        val expected = release.sha256?.takeIf { it.isNotBlank() }
            ?: error("发布信息缺少 SHA-256 校验值，已取消下载")
        val target = File(context.cacheDir, "update-${release.tag}.apk")
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("下载失败：HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        val actual = sha256(target)
        if (!actual.equals(expected, ignoreCase = true)) {
            target.delete()
            error("APK 校验失败：SHA-256 不匹配")
        }
        target
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** 语义化版本比较：vX.Y.Z[-pre]；正式版大于同号预览版。 */
    fun compareVersions(a: String, b: String): Int {
        val (an, ap) = parseVersion(a)
        val (bn, bp) = parseVersion(b)
        for (i in 0 until maxOf(an.size, bn.size)) {
            val x = an.getOrElse(i) { 0 }
            val y = bn.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        if (ap == null && bp == null) return 0
        if (ap == null) return 1
        if (bp == null) return -1
        val aParts = ap.split('.')
        val bParts = bp.split('.')
        for (i in 0 until maxOf(aParts.size, bParts.size)) {
            val x = aParts.getOrElse(i) { "" }
            val y = bParts.getOrElse(i) { "" }
            if (x == y) continue
            if (x.isEmpty()) return -1
            if (y.isEmpty()) return 1
            val xn = x.toIntOrNull()
            val yn = y.toIntOrNull()
            if (xn != null && yn != null) return xn.compareTo(yn)
            if (xn != null) return -1
            if (yn != null) return 1
            return x.compareTo(y)
        }
        return 0
    }

    private fun parseVersion(version: String): Pair<List<Int>, String?> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        val core = cleaned.substringBefore('-')
        val pre = cleaned.substringAfter('-', "").trim().ifBlank { null }
        return core.split('.').map { it.toIntOrNull() ?: 0 } to pre
    }
}
