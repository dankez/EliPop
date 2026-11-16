package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.RotationMode
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiWallpaperRepository
import com.example.data.repository.FirebaseAiWallpaperRepository
import com.example.data.repository.WallpaperRepository
import com.example.wallpaper.RotationScheduler
import com.example.wallpaper.WallpaperApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PastelkaApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepository(
            context = this,
            dao = database.wallpaperDao(),
            wallhavenApi = NetworkModule.wallhavenApi,
            catApi = NetworkModule.catApi,
            dogApi = NetworkModule.dogApi,
        )
    }
    val wallpaperApplier: WallpaperApplier by lazy {
        WallpaperApplier(this, NetworkModule.okHttpClient)
    }
    val rotationScheduler: RotationScheduler by lazy { RotationScheduler(this) }
    val aiWallpaperRepository: AiWallpaperRepository by lazy { FirebaseAiWallpaperRepository() }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            wallpaperRepository.ensureSeeded()
            val prefs = preferencesRepository.getPreferencesOnce()
            if (prefs.rotationMode == RotationMode.ON_UNLOCK) {
                rotationScheduler.syncSchedule(RotationMode.ON_UNLOCK)
            }
        }
    }
}
