package com.screencontrol.shakevolume.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screencontrol.shakevolume.MainActivity
import com.screencontrol.shakevolume.receiver.ScreenStateReceiver
import com.screencontrol.shakevolume.sensor.ShakeDetector
import com.screencontrol.shakevolume.util.PowerUtils
import com.screencontrol.shakevolume.util.PreferencesManager

class ScreenControlForegroundService : Service() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var partialWakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "ScreenCtrlService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_control_service_channel"

        const val ACTION_START = "com.screencontrol.action.START"
        const val ACTION_STOP = "com.screencontrol.action.STOP"
        const val ACTION_SETTINGS_CHANGED = "com.screencontrol.action.SETTINGS_CHANGED"

        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        setupShakeDetector()
        registerScreenStateReceiver()
        acquirePartialWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_SETTINGS_CHANGED -> {
                updateSettings()
            }
            else -> {
                startInForeground()
                startSensorListening()
                isRunning = true
            }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        createNotificationChannel()

        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shake & Volume Screen Control")
            .setContentText("Monitoring motion for screen control")
            .setSmallIcon(android.R.drawable.ic_lock_idle_power)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Control Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps shake-to-wake and screen control active in background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector(
            threshold = preferencesManager.shakeThreshold,
            cooldownMs = preferencesManager.shakeCooldownMs
        ) {
            handleShakeTriggered()
        }
    }

    private fun handleShakeTriggered() {
        if (!preferencesManager.isShakeToWakeEnabled) return

        val isScreenOn = PowerUtils.isScreenInteractive(this)
        Log.i(TAG, "Shake trigger activated! Screen interactive: $isScreenOn")

        if (isScreenOn) {
            PowerUtils.turnScreenOff(this)
        } else {
            PowerUtils.turnScreenOn(this)
        }
    }

    private fun startSensorListening() {
        val sensor = accelerometer ?: return
        shakeDetector?.let { detector ->
            sensorManager.registerListener(detector, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensorListening() {
        shakeDetector?.let { detector ->
            sensorManager.unregisterListener(detector)
        }
    }

    private fun updateSettings() {
        shakeDetector?.apply {
            updateThreshold(preferencesManager.shakeThreshold)
            updateCooldown(preferencesManager.shakeCooldownMs)
        }
    }

    private fun acquirePartialWakeLock() {
        if (partialWakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            partialWakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ShakeVolume:ForegroundServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releasePartialWakeLock() {
        partialWakeLock?.let {
            if (it.isHeld) it.release()
        }
        partialWakeLock = null
    }

    private fun registerScreenStateReceiver() {
        screenStateReceiver = ScreenStateReceiver { isScreenOn ->
            Log.d(TAG, "Screen state broadcast: isScreenOn=$isScreenOn")
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterScreenStateReceiver() {
        screenStateReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) {}
        }
        screenStateReceiver = null
    }

    private fun stopForegroundService() {
        isRunning = false
        stopSensorListening()
        unregisterScreenStateReceiver()
        releasePartialWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}