package com.screencontrol.shakevolume.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.screencontrol.shakevolume.util.PowerUtils
import com.screencontrol.shakevolume.util.PreferencesManager

class ScreenAccessibilityService : AccessibilityService() {

    private lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "ScreenAccessService"
        private var instance: ScreenAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

        fun lockScreen(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        val isVolumeUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val isActionDown = event.action == KeyEvent.ACTION_DOWN

        if (isVolumeUp && isActionDown) {
            val isVolumeWakeEnabled = preferencesManager.isVolumeUpToWakeEnabled

            if (isVolumeWakeEnabled) {
                val isInteractive = PowerUtils.isScreenInteractive(this)
                if (!isInteractive) {
                    Log.i(TAG, "Volume UP received while screen off. Waking screen...")
                    PowerUtils.turnScreenOn(this)
                    return true
                }
            }
        }

        return super.onKeyEvent(event)
    }
}