package com.screencontrol.shakevolume.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.screencontrol.shakevolume.receiver.ScreenAdminReceiver
import com.screencontrol.shakevolume.service.ScreenAccessibilityService

object PowerUtils {

    private const val TAG = "PowerUtils"
    private const val WAKE_LOCK_TAG = "ShakeVolume:ScreenWakeLock"
    private const val WAKE_LOCK_TIMEOUT_MS = 3000L

    fun isScreenInteractive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }

    @Suppress("DEPRECATION")
    fun turnScreenOn(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        try {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                WAKE_LOCK_TAG
            )
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            vibrateFeedback(context)
            Log.i(TAG, "Screen woke up successfully via WakeLock")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn screen ON", e)
        }
    }

    fun turnScreenOff(context: Context): Boolean {
        if (ScreenAccessibilityService.isServiceRunning()) {
            val locked = ScreenAccessibilityService.lockScreen()
            if (locked) {
                vibrateFeedback(context)
                Log.i(TAG, "Screen locked via AccessibilityService GLOBAL_ACTION_LOCK_SCREEN")
                return true
            }
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, ScreenAdminReceiver::class.java)

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            try {
                dpm.lockNow()
                vibrateFeedback(context)
                return true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException invoking lockNow()", e)
            }
        }

        return false
    }

    fun vibrateFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {}
    }
}