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
import com.example.data.remote.WallhavenApi
import com.example.data.remote.WallhavenItem
import com.example.data.remote.WallhavenResponse
import com.example.data.remote.WallhavenThumbs
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
                    order: String,
                ): WallhavenResponse = WallhavenResponse()
            }

        val repo =
            WallpaperRepository(
                context = context,
                dao = db.wallpaperDao(),
                wallhavenApi = fakeWallhaven,
            )

        db.wallpaperDao().upsertAll(
            listOf(
                WallpaperEntity(
                    id = "cat_old_random",
                    title = "Random cat",
                    category = WallpaperCategory.PETS.name,
                    previewUrl = "https://example.com/cat.jpg",
                    fullImageUrl = "https://example.com/cat.jpg",
                    localFilePath = null,
                    drawableName = null,
                    sourceLabel = "TheCatAPI Free",
                ),
                WallpaperEntity(
                    id = "dog_old_random",
                    title = "Random dog",
                    category = WallpaperCategory.PETS.name,
                    previewUrl = "https://example.com/dog.jpg",
                    fullImageUrl = "https://example.com/dog.jpg",
                    localFilePath = null,
                    drawableName = null,
                    sourceLabel = "DogCEO Free",
                ),
            ),
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
    fun `wallhaven failure is returned to caller`() = runTest {
        val repo =
            WallpaperRepository(
                context = context,
                dao = db.wallpaperDao(),
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
                            order: String,
                        ): WallhavenResponse {
                            throw IllegalStateException("Network unavailable")
                        }
                    },
            )

        val result = repo.refreshCategoryFromOnlineBank(WallpaperCategory.NATURE)

        assertTrue(result.isFailure)
    }

    @Test
    fun `wallhaven fills every category with a full result set`() = runTest {
        val expectedSearches =
            listOf(
                Triple(WallpaperCategory.KAWAII, "kawaii anime", "010"),
                Triple(WallpaperCategory.BLACK_GIRLY, "black", "100"),
                Triple(WallpaperCategory.COQUETTE, "bow", "100"),
                Triple(WallpaperCategory.PETS, "cute", "100"),
                Triple(WallpaperCategory.NATURE, "nature", "100"),
                Triple(WallpaperCategory.FLOWERS, "flowers", "100"),
                Triple(WallpaperCategory.PASTEL, "pastel", "100"),
            )
        val expectedCounts =
            mapOf(
                "kawaii anime" to 24,
                "black" to 24,
                "bow" to 24,
                "cute" to 24,
                "nature" to 24,
                "flowers" to 24,
                "pastel" to 8,
                "clouds" to 24,
            )
        val repo =
            WallpaperRepository(
                context = context,
                dao = db.wallpaperDao(),
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
                            order: String,
                        ): WallhavenResponse {
                            val expectedCategory =
                                expectedSearches.firstOrNull { it.second == query }?.third
                                    ?: if (query == "clouds") "100" else null
                            val expectedColor = if (query == "black") "000000" else null
                            val count = expectedCounts[query]
                            if (
                                categories != expectedCategory ||
                                    colors != expectedColor ||
                                    sorting != "views" ||
                                    order != "desc" ||
                                    count == null
                            ) {
                                return WallhavenResponse()
                            }
                            return WallhavenResponse(
                                data =
                                    (1..count).map { index ->
                                        WallhavenItem(
                                            id = "${query.replace(' ', '_')}_$index",
                                            path = "https://example.com/${query.replace(' ', '_')}_$index.jpg",
                                            thumbs =
                                                WallhavenThumbs(
                                                    large =
                                                        "https://example.com/${query.replace(' ', '_')}_$index-thumb.jpg",
                                                ),
                                        )
                                    },
                            )
                        }
                    },
            )

        expectedSearches.forEach { (category, _, _) ->
            val result = repo.refreshCategoryFromOnlineBank(category)
            val expectedCount = if (category == WallpaperCategory.PASTEL) 32 else 24
            assertTrue(result.isSuccess)
            assertEquals(category.name, expectedCount, result.getOrThrow())
            assertEquals(
                category.name,
                expectedCount,
                db.wallpaperDao().getByCategoryOnce(category.name).size,
            )
        }
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
                            order: String,
                        ): WallhavenResponse = WallhavenResponse()
                    },
            )

        val next = repo.pickNextRotationCandidate(prefs)
        assertEquals("w2", next?.id)
    }
}
