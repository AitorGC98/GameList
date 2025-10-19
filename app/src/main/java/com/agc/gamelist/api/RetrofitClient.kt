package com.agc.gamelist.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Constante que contiene la URL base para las peticiones a la API
    private const val BASE_URL = "https://api.rawg.io/api/"

    // Interceptor para registrar el contenido de las respuestas HTTP. Es útil para debugging.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)// Se establece el nivel de logging en BODY para registrar el cuerpo de las respuestas

    }
    // Cliente OkHttp que se utiliza para configurar el interceptor en las peticiones HTTP
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)// Añade el interceptor de logging a las peticiones
        .build()

    // Instancia de Retrofit configurada con la URL base, el cliente y el convertidor de Gson.
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())// Utiliza Gson para convertir las respuestas JSON a objetos Kotlin
        .build()
}
