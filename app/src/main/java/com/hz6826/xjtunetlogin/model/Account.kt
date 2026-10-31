package com.hz6826.xjtunetlogin.model

import java.net.URI

data class Account(
    val username: String = "",
    val password: String = "",
    val domain: String = "@stu",
    val acId: String = "1",
    val baseUrl: String = "http://10.6.21.2",
    val remember: Boolean = false,
    val autoLogin: Boolean = false
) {
    val fullUsername: String get() = if (domain.isEmpty() || username.endsWith(domain)) username else username + domain

    fun validate(requirePassword: Boolean = true) {
        require(username.isNotBlank()) { "请输入校园网账号" }
        if (requirePassword) require(password.isNotEmpty()) { "请输入密码" }
        require(acId.matches(Regex("[0-9]+"))) { "AC ID 应为数字" }
        validatePortal(baseUrl)
    }

    companion object {
        fun validatePortal(baseUrl: String) {
            val uri = runCatching { URI(baseUrl) }.getOrNull()
            require(uri != null && uri.host != null && uri.rawUserInfo == null &&
                uri.rawQuery == null && uri.rawFragment == null && uri.path.orEmpty() in listOf("", "/") &&
                (uri.scheme == "https" || (uri.scheme == "http" && uri.host == "10.6.21.2"))) {
                "认证地址应为 http://10.6.21.2 或 HTTPS 网关根地址"
            }
        }
    }
}
