package com.screencontrol.shakevolume.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.screencontrol.shakevolume.service.ScreenControlForegroundService
import com.screencontrol.shakevolume.util.PreferencesManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != "android.intent.action.QUICKBOOT_POWERON") return
        if (PreferencesManager(context).isShakeToWakeEnabled) {
            val serviceIntent = Intent(context, ScreenControlForegroundService::class.java).apply {
                action = ScreenControlForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, serviceIntent)
            else context.startService(serviceIntent)
        }
    }
}
