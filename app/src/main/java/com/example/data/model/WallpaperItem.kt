package com.example.data.model

data class WallpaperItem(
    val id: String,
    val title: String,
    val category: WallpaperCategory,
    val previewUrl: String?,
    val fullImageUrl: String?,
    val localFilePath: String?,
    val drawableName: String?,
    val isFavorite: Boolean,
    val isUserUploaded: Boolean,
    val sourceLabel: String,
    val accentHex: String,
    val createdAt: Long,
) {
    val hasOfflineImage: Boolean
        get() = !localFilePath.isNullOrBlank() || !drawableName.isNullOrBlank()
}
