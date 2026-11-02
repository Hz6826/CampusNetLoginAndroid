package com.hz6826.campusnetlogin.session

import android.content.Context

/** 界面和后台服务共享同一会话，避免重复认证和注销后的自动重登。 */
object SessionHolder {
    private var session: SessionController? = null

    fun get(context: Context): SessionController =
        session ?: SessionController(context.applicationContext).also { session = it }
}
