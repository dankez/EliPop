package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY isUserUploaded DESC, createdAt DESC")
    fun observeAll(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    suspend fun getFavoritesOnce(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE category = :category ORDER BY createdAt DESC")
    suspend fun getByCategoryOnce(category: String): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE isUserUploaded = 1 ORDER BY createdAt DESC")
    suspend fun getUserUploadedOnce(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WallpaperEntity?

    @Query("SELECT id FROM wallpapers WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT COUNT(*) FROM wallpapers")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WallpaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WallpaperEntity)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteById(id: String)
}
