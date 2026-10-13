package com.screencontrol.shakevolume.util

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("shake_volume_settings", Context.MODE_PRIVATE)

    var isShakeToWakeEnabled: Boolean
        get() = prefs.getBoolean("shake_enabled", false)
        set(value) { prefs.edit().putBoolean("shake_enabled", value).apply() }

    var isVolumeUpToWakeEnabled: Boolean
        get() = prefs.getBoolean("volume_up_enabled", false)
        set(value) { prefs.edit().putBoolean("volume_up_enabled", value).apply() }

    var shakeThreshold: Float
        get() = prefs.getFloat("shake_threshold", 13.5f)
        set(value) { prefs.edit().putFloat("shake_threshold", value).apply() }

    var shakeCooldownMs: Long
        get() = prefs.getLong("shake_cooldown_ms", 2000L)
        set(value) { prefs.edit().putLong("shake_cooldown_ms", value).apply() }
}
