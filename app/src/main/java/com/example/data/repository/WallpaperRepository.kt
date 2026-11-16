package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.data.local.UserPreferences
import com.example.data.local.WallpaperDao
import com.example.data.local.WallpaperEntity
import com.example.data.model.RotationPool
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperItem
import com.example.data.remote.CatApi
import com.example.data.remote.DogApi
import com.example.data.remote.WallhavenApi
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WallpaperRepository(
    private val context: Context,
    private val dao: WallpaperDao,
    private val wallhavenApi: WallhavenApi,
    private val catApi: CatApi,
    private val dogApi: DogApi,
) {
    fun observeAllWallpapers(): Flow<List<WallpaperItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeFavorites(): Flow<List<WallpaperItem>> =
        dao.observeFavorites().map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            val baseTime = System.currentTimeMillis()
            val bundledSeeds =
                listOf(
                    WallpaperEntity(
                        id = "bundled_pets_kitten",
                        title = "Mačiatko v sakurách",
                        category = WallpaperCategory.PETS.name,
                        previewUrl = null,
                        fullImageUrl = null,
                        localFilePath = null,
                        drawableName = "img_wall_pets",
                        isFavorite = true,
                        isUserUploaded = false,
                        sourceLabel = "Kurátorovaná kolekcia",
                        accentHex = "#FF80AB",
                        createdAt = baseTime + 40,
                    ),
                    WallpaperEntity(
                        id = "bundled_nature_meadow",
                        title = "Motýlia lúka",
                        category = WallpaperCategory.NATURE.name,
                        previewUrl = null,
                        fullImageUrl = null,
                        localFilePath = null,
                        drawableName = "img_wall_nature",
                        isFavorite = true,
                        isUserUploaded = false,
                        sourceLabel = "Kurátorovaná kolekcia",
                        accentHex = "#CE93D8",
                        createdAt = baseTime + 30,
                    ),
                    WallpaperEntity(
                        id = "bundled_flowers_peony",
                        title = "Ranné pivonky",
                        category = WallpaperCategory.FLOWERS.name,
                        previewUrl = null,
                        fullImageUrl = null,
                        localFilePath = null,
                        drawableName = "img_wall_floral",
                        isFavorite = false,
                        isUserUploaded = false,
                        sourceLabel = "Kurátorovaná kolekcia",
                        accentHex = "#FF80AB",
                        createdAt = baseTime + 20,
                    ),
                    WallpaperEntity(
                        id = "bundled_pastel_clouds",
                        title = "Cukrové oblaky",
                        category = WallpaperCategory.PASTEL.name,
                        previewUrl = null,
                        fullImageUrl = null,
                        localFilePath = null,
                        drawableName = "img_wall_pastel",
                        isFavorite = false,
                        isUserUploaded = false,
                        sourceLabel = "Kurátorovaná kolekcia",
                        accentHex = "#FFAB91",
                        createdAt = baseTime + 10,
                    ),
                )

            if (dao.countAll() == 0) {
                dao.upsertAll(bundledSeeds)
            } else {
                val favoriteIds = dao.getFavoriteIds().toSet()
                val existingIds = dao.getAllOnce().map { it.id }.toSet()
                val missing =
                    bundledSeeds
                        .filter { it.id !in existingIds }
                        .map { it.copy(isFavorite = it.id in favoriteIds || it.isFavorite) }
                if (missing.isNotEmpty()) {
                    dao.upsertAll(missing)
                }
            }
        }
    }

    suspend fun refreshCategoryFromOnlineBank(
        category: WallpaperCategory,
        page: Int = 1,
        customQuery: String = "",
        colorHex: String? = null,
    ): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val favoriteIds = dao.getFavoriteIds().toSet()
                val fetched = mutableListOf<WallpaperEntity>()
                val now = System.currentTimeMillis()
                val trimmedQuery = customQuery.trim()

                if (trimmedQuery.isNotEmpty() || colorHex != null) {
                    val targetCategory =
                        if (category == WallpaperCategory.ALL || category == WallpaperCategory.CUSTOM) {
                            WallpaperCategory.PASTEL
                        } else {
                            category
                        }
                    val combinedQuery =
                        listOf(trimmedQuery, targetCategory.searchQuery)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    fetched +=
                        fetchWallhavenQuery(
                            query = combinedQuery,
                            categoriesFlag = targetCategory.wallhavenCategories,
                            assignCategory = targetCategory,
                            colorHex = colorHex,
                            page = page,
                            favoriteIds = favoriteIds,
                            now = now,
                        )
                } else {
                    when (category) {
                        WallpaperCategory.ALL -> {
                            coroutineScope {
                                val jobs =
                                    listOf(
                                        WallpaperCategory.KAWAII,
                                        WallpaperCategory.BLACK_GIRLY,
                                        WallpaperCategory.COQUETTE,
                                        WallpaperCategory.PETS,
                                        WallpaperCategory.NATURE,
                                        WallpaperCategory.FLOWERS,
                                        WallpaperCategory.PASTEL,
                                    ).map { cat ->
                                        async { fetchForCategory(cat, page, favoriteIds, now) }
                                    }
                                jobs.forEach { fetched += it.await() }
                            }
                        }

                        WallpaperCategory.CUSTOM -> Unit

                        else -> {
                            fetched += fetchForCategory(category, page, favoriteIds, now)
                        }
                    }
                }

                if (fetched.isNotEmpty()) {
                    dao.upsertAll(fetched.distinctBy { it.id })
                }
                fetched.size
            }
        }

    private suspend fun fetchForCategory(
        category: WallpaperCategory,
        page: Int,
        favoriteIds: Set<String>,
        now: Long,
    ): List<WallpaperEntity> {
        val items = mutableListOf<WallpaperEntity>()
        val colorFilter = if (category == WallpaperCategory.BLACK_GIRLY) "000000" else null

        val primaryItems =
            fetchWallhavenQuery(
                query = category.searchQuery,
                categoriesFlag = category.wallhavenCategories,
                assignCategory = category,
                colorHex = colorFilter,
                page = page,
                favoriteIds = favoriteIds,
                now = now,
            )
        items += primaryItems

        if (primaryItems.size < WALLHAVEN_PAGE_SIZE) {
            for (fallbackQuery in category.fallbackSearchQueries) {
                items +=
                    fetchWallhavenQuery(
                        query = fallbackQuery,
                        categoriesFlag = category.wallhavenCategories,
                        assignCategory = category,
                        colorHex = colorFilter,
                        page = page,
                        favoriteIds = favoriteIds,
                        now = now,
                    )
                if (items.distinctBy { it.id }.size >= WALLHAVEN_PAGE_SIZE) break
            }
        }

        if (category == WallpaperCategory.PETS) {
            coroutineScope {
                val cats = async { fetchOptional("cat") { catApi.getRandomCats(limit = 6) } }
                val dogs = async { fetchOptional("dog", dogApi::getRandomDogs) }

                cats.await()?.forEachIndexed { idx, cat ->
                    val id = "cat_${cat.id}"
                    items +=
                        WallpaperEntity(
                            id = id,
                            title = "Rozkošná mačička",
                            category = WallpaperCategory.PETS.name,
                            previewUrl = cat.url,
                            fullImageUrl = cat.url,
                            localFilePath = null,
                            drawableName = null,
                            isFavorite = id in favoriteIds,
                            isUserUploaded = false,
                            sourceLabel = "TheCatAPI Free",
                            accentHex = "#FF80AB",
                            createdAt = now - 20 - idx,
                        )
                }

                dogs.await()?.message?.take(6)?.forEachIndexed { idx, url ->
                    val slug = url.substringAfterLast('/').substringBefore('.')
                    val id = "dog_${slug}_${url.hashCode()}"
                    items +=
                        WallpaperEntity(
                            id = id,
                            title = "Milé šteniatko",
                            category = WallpaperCategory.PETS.name,
                            previewUrl = url,
                            fullImageUrl = url,
                            localFilePath = null,
                            drawableName = null,
                            isFavorite = id in favoriteIds,
                            isUserUploaded = false,
                            sourceLabel = "DogCEO Free",
                            accentHex = "#FFAB91",
                            createdAt = now - 40 - idx,
                        )
                }
            }
        }

        return items
    }

    private suspend fun fetchWallhavenQuery(
        query: String,
        categoriesFlag: String,
        assignCategory: WallpaperCategory,
        colorHex: String?,
        page: Int,
        favoriteIds: Set<String>,
        now: Long,
    ): List<WallpaperEntity> {
        val result = mutableListOf<WallpaperEntity>()
        val response =
            wallhavenApi.searchWallpapers(
                query = query,
                categories = categoriesFlag,
                colors = colorHex,
                page = page,
            )
        response.data.forEachIndexed { idx, wh ->
            val id = "wh_${wh.id}"
            result +=
                WallpaperEntity(
                    id = id,
                    title = "${assignCategory.displayName} #${wh.id.take(4).uppercase()}",
                    category = assignCategory.name,
                    previewUrl = wh.thumbs.large,
                    fullImageUrl = wh.path,
                    localFilePath = null,
                    drawableName = null,
                    isFavorite = id in favoriteIds,
                    isUserUploaded = false,
                    sourceLabel = "Wallhaven Free Bank",
                    accentHex = wh.colors.firstOrNull() ?: "#FF80AB",
                    createdAt = now - idx,
                )
        }
        return result
    }

    private suspend fun <T : Any> fetchOptional(
        source: String,
        fetch: suspend () -> T,
    ): T? =
        try {
            fetch()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Failed to fetch $source wallpapers", error)
            null
        }

    private companion object {
        const val TAG = "WallpaperRepository"
        const val WALLHAVEN_PAGE_SIZE = 24
    }

    suspend fun importUserWallpapers(uris: List<Uri>): Result<List<WallpaperItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(context.filesDir, "user_wallpapers").apply { mkdirs() }
                val imported = mutableListOf<WallpaperEntity>()

                uris.forEachIndexed { index, uri ->
                    val fileName =
                        "custom_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                    val destFile = File(folder, fileName)
                    val copied =
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                            true
                        } ?: false

                    if (copied && destFile.length() > 0L) {
                        val id = "user_${UUID.randomUUID()}"
                        val entity =
                            WallpaperEntity(
                                id = id,
                                title = "Moja fotka ${index + 1}",
                                category = WallpaperCategory.CUSTOM.name,
                                previewUrl = null,
                                fullImageUrl = null,
                                localFilePath = destFile.absolutePath,
                                drawableName = null,
                                isFavorite = true,
                                isUserUploaded = true,
                                sourceLabel = "Moja galéria",
                                accentHex = "#FF80AB",
                                createdAt = System.currentTimeMillis() + index,
                            )
                        dao.upsert(entity)
                        imported += entity
                    }
                }

                imported.map { it.toDomain() }
            }
        }

    suspend fun saveEditedBitmapToLibrary(
        bitmap: Bitmap,
        title: String,
        sourceLabel: String = "DIY Štúdio",
    ): Result<WallpaperItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(context.filesDir, "user_wallpapers").apply { mkdirs() }
                val fileName =
                    "diy_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                val destFile = File(folder, fileName)
                destFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                }
                val entity =
                    WallpaperEntity(
                        id = "diy_${UUID.randomUUID()}",
                        title = title.ifBlank { "Moja úprava" },
                        category = WallpaperCategory.CUSTOM.name,
                        previewUrl = null,
                        fullImageUrl = null,
                        localFilePath = destFile.absolutePath,
                        drawableName = null,
                        isFavorite = true,
                        isUserUploaded = true,
                        sourceLabel = sourceLabel,
                        accentHex = "#FF80AB",
                        createdAt = System.currentTimeMillis(),
                    )
                dao.upsert(entity)
                entity.toDomain()
            }
        }

    suspend fun toggleFavorite(id: String, currentFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            dao.setFavorite(id, !currentFavorite)
        }
    }

    suspend fun deleteWallpaper(item: WallpaperItem) {
        withContext(Dispatchers.IO) {
            if (item.isUserUploaded && !item.localFilePath.isNullOrBlank()) {
                runCatching { File(item.localFilePath).delete() }
            }
            dao.deleteById(item.id)
        }
    }

    suspend fun getById(id: String): WallpaperItem? =
        withContext(Dispatchers.IO) { dao.getById(id)?.toDomain() }

    suspend fun getEligibleRotationPool(prefs: UserPreferences): List<WallpaperItem> =
        withContext(Dispatchers.IO) {
            val primary =
                when (prefs.rotationPool) {
                    RotationPool.FAVORITES -> dao.getFavoritesOnce()
                    RotationPool.CUSTOM_ONLY -> dao.getUserUploadedOnce()
                    RotationPool.CATEGORY -> dao.getByCategoryOnce(prefs.rotationCategory.name)
                    RotationPool.ALL_SAVED -> dao.getAllOnce()
                }
            val effective = primary.ifEmpty { dao.getAllOnce() }
            effective.map { it.toDomain() }
        }

    suspend fun pickNextRotationCandidate(prefs: UserPreferences): WallpaperItem? {
        val pool = getEligibleRotationPool(prefs)
        if (pool.isEmpty()) return null
        if (pool.size == 1) return pool.first()
        val candidates = pool.filter { it.id != prefs.currentWallpaperId }
        return (candidates.ifEmpty { pool }).random()
    }
}
