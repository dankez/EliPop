package com.example.wallpaper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PastelkaApp

class WallpaperRotationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PastelkaApp ?: return Result.failure()
        app.wallpaperRepository.ensureSeeded()
        val prefs = app.preferencesRepository.getPreferencesOnce()
        val next = app.wallpaperRepository.pickNextRotationCandidate(prefs) ?: return Result.success()

        val result = app.wallpaperApplier.applyWallpaper(next, prefs.rotationTarget)
        return if (result.isSuccess) {
            app.preferencesRepository.recordWallpaperApplied(
                wallpaperId = next.id,
                incrementRotationCount = true,
            )
            Result.success()
        } else {
            Result.retry()
        }
    }
}
