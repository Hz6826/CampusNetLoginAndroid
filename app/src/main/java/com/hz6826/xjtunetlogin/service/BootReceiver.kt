package com.hz6826.xjtunetlogin.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hz6826.xjtunetlogin.data.AccountStore
import com.hz6826.xjtunetlogin.data.SettingsStore
import com.hz6826.xjtunetlogin.model.Account
import kotlin.concurrent.thread

/**
 * 开机自启：与「前台保活与自动登录」开关保持一致，没有单独的开关。
 * 需要系统允许自启动（国产 ROM 通常要手动授权），否则收不到该广播。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        // Keystore 解密不能放在广播主线程上，开机高峰会拖慢系统。
        thread(name = "boot-autostart") {
            try {
                if (!SettingsStore(context).load().keepAlive) return@thread
                val account = runCatching { AccountStore(context).load() }.getOrDefault(Account())
                if (!account.remember || account.username.isBlank() || account.password.isBlank()) return@thread
                context.startForegroundService(Intent(context, AutoLoginService::class.java))
            } catch (e: Exception) {
                Log.w("BootReceiver", "开机自启失败", e)
            } finally {
                pending.finish()
            }
        }
    }
}
