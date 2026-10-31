package com.hz6826.xjtunetlogin.ui.pages

import com.hz6826.xjtunetlogin.model.Account
import androidx.compose.runtime.saveable.listSaver

/** 界面里正在编辑的账户/连接设置草稿。 */
data class Draft(
    val username: String = "",
    val password: String = "",
    val domain: String = "@stu",
    val acId: String = "1",
    val baseUrl: String = "http://10.6.21.2",
    val remember: Boolean = false,
    val loaded: Boolean = false,
)

val draftSaver = listSaver<Draft, Any>(
    save = {
        listOf(it.username, it.password, it.domain, it.acId, it.baseUrl, it.remember, it.loaded)
    },
    restore = {
        Draft(
            username = it[0] as String,
            password = it[1] as String,
            domain = it[2] as String,
            acId = it[3] as String,
            baseUrl = it[4] as String,
            remember = it[5] as Boolean,
            loaded = it[6] as Boolean,
        )
    },
)

fun accountOf(draft: Draft) = Account(
    username = draft.username.trim(),
    password = draft.password,
    domain = draft.domain.trim(),
    acId = draft.acId.trim(),
    baseUrl = draft.baseUrl.trim().trimEnd('/'),
    remember = draft.remember,
)
