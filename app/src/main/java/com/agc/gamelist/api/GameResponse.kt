package com.agc.gamelist.api

import com.agc.gamelist.model.Game

/**
 * Clase que representa una respuesta de juegos.
 * Almacena la lista de juegos
 */
data class GameResponse(
    val results: List<Game>
)