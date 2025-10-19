package com.agc.gamelist.model

/**
 * Clase que representa un juego.
 * Solo usada para almacenar datos
 */
data class Game(
    val id: Int,
    val name: String,
    val released: String?,              // Fecha de lanzamiento
    val background_image: String?,      // Imagen del fondo
    val rating: Double,                 // Puntuación del juego
    val website: String?,               // Página web oficial
    val metacritic: Int?,               // Puntuación en Metacritic
    val description_raw: String?,       // Descripción del juego (formato raw)
    val publishers: List<Publisher>?,   // Lista de publishers (editoras)
    val genres: List<Genre>?,           // Lista de géneros del juego
    val platforms: List<Platform>?,     // Lista de plataformas en las que está disponible
    val playtime: Int?,                 // Tiempo de juego promedio
    val clip: Clip?,
    val developers: List<Developer>? // Asegúrate de incluir esto
)

data class PublisherResponse(
    val results: List<Publisher>
)


data class Developer(
    val id: Int,
    val name: String
)

data class Publisher(
    val id: Int,
    val name: String
)

data class Genre(
    val id: Int,
    val name: String
)

data class Platform(
    val platform: PlatformInfo
)

data class PlatformInfo(
    val id: Int,
    val name: String
)

data class Clip(
    val clip: String?        // Enlace al video o trailer
)
