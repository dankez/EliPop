package com.example.data.remote

import com.squareup.moshi.Json
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

@JsonClass(generateAdapter = true)
data class CatApiItem(
    val id: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

@JsonClass(generateAdapter = true)
data class DogCeoResponse(
    val message: List<String> = emptyList(),
    val status: String = "",
)
