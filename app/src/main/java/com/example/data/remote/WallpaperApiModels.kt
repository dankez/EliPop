package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WallhavenResponse(
    val data: List<WallhavenItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WallhavenItem(
    val id: String,
    val path: String,
    val thumbs: WallhavenThumbs,
    val colors: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WallhavenThumbs(
    val large: String,
    val original: String? = null,
    val small: String? = null,
)
