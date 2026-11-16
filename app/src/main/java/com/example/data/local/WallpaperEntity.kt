package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperItem

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val previewUrl: String?,
    val fullImageUrl: String?,
    val localFilePath: String?,
    val drawableName: String?,
    val isFavorite: Boolean = false,
    val isUserUploaded: Boolean = false,
    val sourceLabel: String,
    val accentHex: String = "#F48FB1",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomain(): WallpaperItem =
        WallpaperItem(
            id = id,
            title = title,
            category = WallpaperCategory.fromName(category),
            previewUrl = previewUrl,
            fullImageUrl = fullImageUrl,
            localFilePath = localFilePath,
            drawableName = drawableName,
            isFavorite = isFavorite,
            isUserUploaded = isUserUploaded,
            sourceLabel = sourceLabel,
            accentHex = accentHex,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(item: WallpaperItem): WallpaperEntity =
            WallpaperEntity(
                id = item.id,
                title = item.title,
                category = item.category.name,
                previewUrl = item.previewUrl,
                fullImageUrl = item.fullImageUrl,
                localFilePath = item.localFilePath,
                drawableName = item.drawableName,
                isFavorite = item.isFavorite,
                isUserUploaded = item.isUserUploaded,
                sourceLabel = item.sourceLabel,
                accentHex = item.accentHex,
                createdAt = item.createdAt,
            )
    }
}
