package com.agc.gamelist.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.rawg.io/api/")// Establece la URL base para las solicitudes
            .addConverterFactory(GsonConverterFactory.create())// Añade el convertidor Gson para convertir JSON a objetos
            .build()// Construye la instancia de Retrofit
    }

}