package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.WallpaperEntity
import com.example.data.model.RotationMode
import com.example.data.model.RotationPool
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperTarget
import com.example.data.remote.CatApi
import com.example.data.remote.CatApiItem
import com.example.data.remote.DogApi
import com.example.data.remote.DogCeoResponse
import com.example.data.remote.WallhavenApi
import com.example.data.remote.WallhavenResponse
import com.example.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read app name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("EliPop", appName)
    }

    @Test
    fun `repository seeds offline wallpapers and preserves favorites`() = runTest {
        val fakeWallhaven =
            object : WallhavenApi {
                override suspend fun searchWallpapers(
                    query: String,
                    categories: String,
                    purity: String,
                    ratios: String,
                    colors: String?,
                    sorting: String,
                    page: Int,
                ): WallhavenResponse = WallhavenResponse()
            }
        val fakeCat =
            object : CatApi {
                override suspend fun getRandomCats(limit: Int, mimeTypes: String): List<CatApiItem> =
                    emptyList()
            }
        val fakeDog =
            object : DogApi {
                override suspend fun getRandomDogs(): DogCeoResponse = DogCeoResponse()
            }

        val repo =
            WallpaperRepository(
                context = context,
                dao = db.wallpaperDao(),
                wallhavenApi = fakeWallhaven,
                catApi = fakeCat,
                dogApi = fakeDog,
            )

        repo.ensureSeeded()
        val all = repo.observeAllWallpapers().first()
        assertEquals(4, all.size)
        assertTrue(all.any { it.category == WallpaperCategory.PETS })
        assertTrue(all.any { it.category == WallpaperCategory.NATURE })

        val favTarget = all.first { !it.isFavorite }
        repo.toggleFavorite(favTarget.id, favTarget.isFavorite)
        val updated = repo.getById(favTarget.id)
        assertNotNull(updated)
        assertTrue(updated!!.isFavorite)
    }

    @Test
    fun `rotation candidate respects pool and avoids immediate repeat`() = runTest {
        val dao = db.wallpaperDao()
        dao.upsertAll(
            listOf(
                WallpaperEntity(
                    id = "w1",
                    title = "Prvá",
                    category = WallpaperCategory.PETS.name,
                    previewUrl = null,
                    fullImageUrl = null,
                    localFilePath = null,
                    drawableName = "img_wall_pets",
                    isFavorite = true,
                    isUserUploaded = false,
                    sourceLabel = "Test",
                ),
                WallpaperEntity(
                    id = "w2",
                    title = "Druhá",
                    category = WallpaperCategory.PETS.name,
                    previewUrl = null,
                    fullImageUrl = null,
                    localFilePath = null,
                    drawableName = "img_wall_nature",
                    isFavorite = true,
                    isUserUploaded = false,
                    sourceLabel = "Test",
                ),
            ),
        )

        val prefs =
            com.example.data.local.UserPreferences(
                themeMode = ThemeMode.DARK,
                rotationMode = RotationMode.ON_UNLOCK,
                rotationTarget = WallpaperTarget.LOCK,
                rotationPool = RotationPool.FAVORITES,
                currentWallpaperId = "w1",
            )
        val repo =
            WallpaperRepository(
                context = context,
                dao = dao,
                wallhavenApi =
                    object : WallhavenApi {
                        override suspend fun searchWallpapers(
                            query: String,
                            categories: String,
                            purity: String,
                            ratios: String,
                            colors: String?,
                            sorting: String,
                            page: Int,
                        ): WallhavenResponse = WallhavenResponse()
                    },
                catApi =
                    object : CatApi {
                        override suspend fun getRandomCats(limit: Int, mimeTypes: String): List<CatApiItem> =
                            emptyList()
                    },
                dogApi =
                    object : DogApi {
                        override suspend fun getRandomDogs(): DogCeoResponse = DogCeoResponse()
                    },
            )

        val next = repo.pickNextRotationCandidate(prefs)
        assertEquals("w2", next?.id)
    }
}
