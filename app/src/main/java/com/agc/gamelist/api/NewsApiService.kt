package com.agc.gamelist.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object NewsApiService {
    private const val BASE_URL = "https://newsapi.org/v2/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: NewsApi by lazy {
        retrofit.create(NewsApi::class.java)
    }
}

interface NewsApi {
    @GET("everything")
    suspend fun getGamingNews(
        @Query("q") query: String = "video games", // Búsqueda por videojuegos
        @Query("language") language: String = "es", // Filtrar solo en español
        @Query("pageSize") pageSize: Int = 34, // Obtener las 30 más recientes
        @Query("sortBy") sortBy: String = "publishedAt", // Ordenar por las más recientes
        @Query("apiKey") apiKey: String // Clave de API
    ): NewsResponse
}

