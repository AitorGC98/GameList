package com.agc.gamelist.network

import com.agc.gamelist.api.GameResponse
import com.agc.gamelist.model.Game
import com.agc.gamelist.model.PublisherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApi {
    // Define una función suspendida que realiza una solicitud GET a la ruta "games" de la API
    @GET("games")
    suspend fun getGames(
        @Query("key") apiKey: String,  // API key como parámetro
        @Query("page") page: Int,// Parámetro de consulta que indica el número de página
        @Query("page_size") pageSize: Int// Parámetro de consulta que indica el tamaño de la página (número de juegos por página)
    ): Response<GameResponse>// La respuesta esperada es un objeto de tipo Response<GameResponse>

    // Método para buscar juegos por título
    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("search") query: String // Parámetro de búsqueda
    ): Response<GameResponse>

    // Método para obtener los detalles del juego por ID
    @GET("games/{id}")
    suspend fun getGameDetails(
        @Path("id") gameId: Int,          // ID del juego
        @Query("key") apiKey: String      // API key como parámetro
    ): Response<Game>                    // Devuelve un objeto `Game` con todos los campos

    @GET("games")
    suspend fun getGamesReleasedThisMonth(
        @Query("key") apiKey: String,
        @Query("dates") dates: String,       // Fechas del mes actual
        @Query("ordering") ordering: String = "-added,-rating", // Orden descendente por fecha de adición
        @Query("page_size") pageSize: Int = 30 // Solo 10 juegos
    ): Response<GameResponse> // La respuesta esperada es un objeto `GameResponse`


    @GET("games")
    suspend fun getUpcomingGames(
        @Query("key") apiKey: String,
        @Query("dates") dates: String, // Fechas futuras
        @Query("page_size") pageSize: Int = 10,
        @Query("ordering") ordering: String = "released,rating" // Ordenar por fecha de lanzamiento
    ): Response<GameResponse>

    @GET("games")
    suspend fun getTopGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-metacritic", // Ordenar por rating
        @Query("page_size") pageSize: Int = 10 // Tamaño de página
    ): Response<GameResponse>

    // En la interfaz de la API
    @GET("games")
    suspend fun getShortGames(
        @Query("key") apiKey: String,
        @Query("tags") tags: String="short",// Término de búsqueda
        @Query("ordering") ordering: String = "-metacritic", // Ordenar por calificación
        @Query("page_size") pageSize: Int = 10 // Tamaño de página
    ): Response<GameResponse>

    @GET("games")
    suspend fun getTopUbisoftGames(
        @Query("key") apiKey: String,
        @Query("publishers") publishers: String = "918", // Usar el ID de Ubisoft
        @Query("ordering") ordering: String = "-metacritic,-released",
        @Query("page_size") pageSize: Int = 10
    ): Response<GameResponse>

    @GET("games")
    suspend fun getSteamLeaderboardGames(
        @Query("key") apiKey: String,
        @Query("tags") tags: String = "steam-leaderboards", // Término de búsqueda para Steam Leaderboards Games
        @Query("ordering") ordering: String = "-metacritic", // Ordenar por calificación
        @Query("page_size") pageSize: Int = 10 // Tamaño de página
    ): Response<GameResponse>


    @GET("games")
    suspend fun getMultiplayerGames(
        @Query("key") apiKey: String,
        @Query("tags") tags: String = "multiplayer", // Filtrar por género multijugador
        @Query("dates") dates: String, // Filtrar por fechas (a establecer en tiempo de ejecución)
        @Query("ordering") ordering: String = "-metacritic,released", // Ordenar por calificación y fecha de lanzamiento
        @Query("page_size") pageSize: Int = 10 // Tamaño de la página
    ): Response<GameResponse>

    @GET("games")
    suspend fun getReleasedThisYear(
        @Query("key") apiKey: String,
        @Query("dates") dates: String, // Rango de fechas para el año actual
        @Query("ordering") ordering: String = "-rating", // Ordenar por fecha de lanzamiento
        @Query("page_size") pageSize: Int = 10 // Tamaño de la página
    ): Response<GameResponse>



        @GET("games")
        suspend fun getGamesByPublisher(
            @Query("key") apiKey: String,
            @Query("publishers") publisherId: String,
            @Query("dates") dates: String,
            @Query("page_size") pageSize: Int = 10
        ): Response<GameResponse>


}