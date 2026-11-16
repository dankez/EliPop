package com.example.wallpaper

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.RotationMode
import com.example.service.UnlockWallpaperService
import java.util.concurrent.TimeUnit

class RotationScheduler(private val context: Context) {

    fun syncSchedule(mode: RotationMode) {
        val workManager = WorkManager.getInstance(context)

        when (mode) {
            RotationMode.STATIC -> {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                stopUnlockService()
            }

            RotationMode.ON_UNLOCK -> {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                startUnlockService()
            }

            RotationMode.INTERVAL_15M,
            RotationMode.INTERVAL_1H,
            RotationMode.INTERVAL_6H,
            RotationMode.INTERVAL_24H -> {
                stopUnlockService()
                val minutes = mode.intervalMinutes ?: 60L
                val request =
                    PeriodicWorkRequestBuilder<WallpaperRotationWorker>(minutes, TimeUnit.MINUTES)
                        .build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }
    }

    private fun startUnlockService() {
        val intent = Intent(context, UnlockWallpaperService::class.java)
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    private fun stopUnlockService() {
        val intent = Intent(context, UnlockWallpaperService::class.java)
        runCatching { context.stopService(intent) }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "pastelka_periodic_rotation"
    }
}
