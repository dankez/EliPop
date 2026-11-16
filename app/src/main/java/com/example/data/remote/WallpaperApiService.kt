package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApi {
    @GET("api/v1/search")
    suspend fun searchWallpapers(
        @Query("q") query: String,
        @Query("categories") categories: String = "110",
        @Query("purity") purity: String = "100",
        @Query("ratios") ratios: String = "portrait",
        @Query("colors") colors: String? = null,
        @Query("sorting") sorting: String = "views",
        @Query("page") page: Int = 1,
        @Query("order") order: String = "desc",
    ): WallhavenResponse
}
