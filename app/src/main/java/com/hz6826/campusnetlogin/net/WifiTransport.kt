package com.hz6826.campusnetlogin.net

import android.net.Network
import com.hz6826.campusnetlogin.model.Account
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.net.URI

/**
 * 校园网 HTTP 传输层。
 *
 * @param onHttp 可选的 HTTP 日志回调，只传方法/路径/状态码，不含 query 或请求体（避免泄漏凭据）。
 */
class WifiTransport(
    private val network: Network,
    private val baseUrl: String,
    private val timeoutMs: Int = 8000,
    private val onHttp: ((String) -> Unit)? = null,
) : PortalTransport {
    private var cancelled = false
    private var activeConnection: HttpURLConnection? = null

    @Synchronized fun cancel() {
        cancelled = true
        activeConnection?.disconnect()
    }

    override fun get(path: String, params: Map<String, String>): String {
        Account.validatePortal(baseUrl)
        val query = params.entries.joinToString("&") { encode(it.key) + "=" + encode(it.value) }
        val url = URL(baseUrl.trimEnd('/') + path + if (query.isEmpty()) "" else "?$query")
        return connection(url).useConnection { connection ->
            connection.setRequestProperty("User-Agent", "CampusNetLogin/1.0 Android")
            connection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            connection.setRequestProperty("Referer", "${baseUrl.trimEnd('/')}/srun_portal_pc")
            connection.setRequestProperty("Origin", baseUrl.trimEnd('/'))
            val code = connection.responseCode
            onHttp?.invoke("GET $path -> HTTP $code")
            if (code != 200) throw IOException("认证服务器返回 HTTP $code")
            // 限制响应体大小，避免劫持页面占满内存；错误信息中不包含带凭据的 URL。
            connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 262144) throw IOException("认证响应过大")
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                bytes.toString(Charsets.UTF_8)
            }
        }
    }

    fun checkInternet(): Boolean = connection(URL(GENERATE_204))
        .useConnection { connection ->
            val code = connection.responseCode
            onHttp?.invoke("GET /generate_204 -> HTTP $code")
            code == 204
        }

    fun isCampusPortalRedirect(): Boolean = connection(URL(GENERATE_204))
        .useConnection { connection ->
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            // 只记录目标和路径，避免把 redirect 里的 token/查询参数写进用户可见日志。
            val safeLocation = location?.substringBefore('?')?.substringBefore('#')
            onHttp?.invoke("GET /generate_204 -> HTTP $code" + if (safeLocation.isNullOrBlank()) "" else " -> $safeLocation")
            isConfiguredPortalRedirect(code, location, baseUrl)
        }

    @Synchronized private fun connection(url: URL): HttpURLConnection {
        if (cancelled) throw InterruptedIOException("网络已变化")
        return (network.openConnection(url, Proxy.NO_PROXY) as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
            useCaches = false
            activeConnection = this
        }
    }

    private fun <T> HttpURLConnection.useConnection(block: (HttpURLConnection) -> T): T =
        try { block(this) } finally {
            disconnect()
            synchronized(this@WifiTransport) { if (activeConnection === this) activeConnection = null }
        }

    companion object {
        private const val GENERATE_204 = "http://firefox-portal-detection.com/generate_204"

        internal fun isConfiguredPortalRedirect(code: Int, location: String?, baseUrl: String): Boolean {
            if (code !in 300..399 || location.isNullOrBlank()) return false
            val portal = runCatching { URI(baseUrl) }.getOrNull() ?: return false
            val redirect = runCatching { URI(location) }.getOrNull() ?: return false
            fun port(uri: URI) = if (uri.port != -1) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
            return redirect.isAbsolute && redirect.rawUserInfo == null &&
                redirect.scheme.equals(portal.scheme, true) && redirect.host != null &&
                redirect.host.equals(portal.host, true) && port(redirect) == port(portal)
        }

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
