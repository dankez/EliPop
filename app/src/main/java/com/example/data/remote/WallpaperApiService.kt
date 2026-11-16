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
        @Query("sorting") sorting: String = "favorites",
        @Query("page") page: Int = 1,
    ): WallhavenResponse
}

interface CatApi {
    @GET("https://api.thecatapi.com/v1/images/search")
    suspend fun getRandomCats(
        @Query("limit") limit: Int = 8,
        @Query("mime_types") mimeTypes: String = "jpg,png",
    ): List<CatApiItem>
}

interface DogApi {
    @GET("https://dog.ceo/api/breeds/image/random/8")
    suspend fun getRandomDogs(): DogCeoResponse
}
