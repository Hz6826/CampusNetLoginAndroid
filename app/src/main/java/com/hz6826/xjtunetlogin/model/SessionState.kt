package com.hz6826.xjtunetlogin.model

/** 当前 Wi-Fi 是否处于校园网环境。OffCampus 时界面上显示「无需连接」。 */
enum class CampusState { Unknown, OnCampus, OffCampus }

data class SessionState(
    val busy: Boolean = false,
    val status: String = "连接校园 Wi-Fi 后即可登录",
    val online: OnlineInfo? = null,
    val campus: CampusState = CampusState.Unknown,
)
