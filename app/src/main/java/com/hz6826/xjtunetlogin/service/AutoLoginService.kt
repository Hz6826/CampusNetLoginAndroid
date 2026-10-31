package com.hz6826.xjtunetlogin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.hz6826.xjtunetlogin.MainActivity
import com.hz6826.xjtunetlogin.R
import com.hz6826.xjtunetlogin.model.CampusState
import com.hz6826.xjtunetlogin.model.SessionState
import com.hz6826.xjtunetlogin.model.formatDuration
import com.hz6826.xjtunetlogin.session.SessionController
import com.hz6826.xjtunetlogin.session.SessionHolder

/**
 * 前台保活服务：常驻通知显示当前校园网状态、账号与已用时长，
 * 通知里可以直接登录 / 注销 / 关闭应用。
 */
class AutoLoginService : Service() {
    private lateinit var controller: SessionController
    private var notifications: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        notifications?.createNotificationChannel(
            NotificationChannel(CHANNEL, "校园网状态与保活", NotificationManager.IMPORTANCE_LOW).apply {
                description = "显示校园网认证状态，并提供登录、注销和关闭应用操作"
                setShowBadge(false)
            },
        )
        // 先立刻 startForeground（用最简单的占位通知），避免后台启动时因初始化慢被判超时。
        startForeground(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("正在启动…")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build(),
        )
        controller = SessionHolder.get(this)
        controller.setBackgroundActive(true)
        controller.stateObserver = { updateNotification() }
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOGOUT -> controller.logout(controller.account)
            ACTION_LOGIN -> {
                val account = controller.account
                if (account.username.isNotBlank() && account.password.isNotBlank()) {
                    controller.login(account)
                } else {
                    openApp()
                }
            }
            ACTION_CLOSE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Process.killProcess(Process.myPid())
            }
            else -> updateNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        controller.stateObserver = null
        controller.setBackgroundActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        notifications?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = controller.state
        val online = state.online
        // 标题用稳定的连接状态，不用「设置已保存」这类临时操作提示。
        val title = when {
            online != null -> "已连接校园网"
            state.campus == CampusState.OffCampus -> "无需登录"
            state.campus == CampusState.OnCampus -> "校园网未登录"
            else -> "等待连接"
        }
        val accountName = controller.account.username.ifBlank { "未设置账号" }
        // 第一行账号，第二行已用时长（离线时显示当前状态）。
        val firstLine = "账号：$accountName"
        val secondLine = if (online != null) {
            "已用时长：${formatDuration(online.usedSeconds)}"
        } else {
            state.status
        }
        val detail = firstLine + "\n" + secondLine
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(firstLine)
            .setStyle(Notification.BigTextStyle().bigText(detail))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
        when {
            online != null -> builder.addAction(action("注销", ACTION_LOGOUT))
            state.campus == CampusState.OnCampus -> builder.addAction(action("登录", ACTION_LOGIN))
        }
        builder.addAction(action("关闭应用", ACTION_CLOSE))
        return builder.build()
    }

    private fun action(title: String, action: String): Notification.Action {
        val intent = Intent(this, AutoLoginService::class.java).setAction(action)
        val pending = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, title, pending).build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        private const val CHANNEL = "campus_auto_login"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_LOGIN = "com.hz6826.xjtunetlogin.LOGIN"
        private const val ACTION_LOGOUT = "com.hz6826.xjtunetlogin.LOGOUT"
        private const val ACTION_CLOSE = "com.hz6826.xjtunetlogin.CLOSE"
    }
}
