package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.PastelkaApp
import com.example.R
import com.example.data.model.RotationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UnlockWallpaperService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiverRegistered = false

    private val unlockReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    val pendingResult = goAsync()
                    serviceScope.launch {
                        try {
                            rotateNowOnUnlock()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)

        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            registerReceiver(unlockReceiver, filter)
            receiverRegistered = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ROTATE_NOW) {
            serviceScope.launch { rotateNowOnUnlock() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(unlockReceiver) }
            receiverRegistered = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun rotateNowOnUnlock() {
        val app = applicationContext as? PastelkaApp ?: return
        app.wallpaperRepository.ensureSeeded()
        val prefs = app.preferencesRepository.getPreferencesOnce()
        if (prefs.rotationMode != RotationMode.ON_UNLOCK) {
            stopSelf()
            return
        }
        val next = app.wallpaperRepository.pickNextRotationCandidate(prefs) ?: return
        val result = app.wallpaperApplier.applyWallpaper(next, prefs.rotationTarget)
        if (result.isSuccess) {
            app.preferencesRepository.recordWallpaperApplied(
                wallpaperId = next.id,
                incrementRotationCount = true,
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        description = getString(R.string.notification_channel_desc)
                        setShowBadge(false)
                    }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val shuffleIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, UnlockWallpaperService::class.java).apply { action = ACTION_ROTATE_NOW },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_unlock_title))
            .setContentText(getString(R.string.notification_unlock_text))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.action_rotate_now), shuffleIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "pastelka_unlock_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_ROTATE_NOW = "com.example.pastelka.ACTION_ROTATE_NOW"
    }
}
