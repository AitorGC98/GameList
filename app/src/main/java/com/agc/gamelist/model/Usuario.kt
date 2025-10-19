package com.agc.gamelist.model

import java.time.LocalDate
import java.util.Date

data class Usuario(
    val nombre: String = "",
    val correo: String = "",
    val fotoPerfil: String = "",
    val foros: List<String> = emptyList(), // Lista de IDs de foros
    var experienciaActual: Int = 0, // Cambiado a var
    var nivel: Int = 1, // Cambiado a var
    var sobreTi:String="",
    var privado:Boolean=false,
    var experienciaNecesaria: Int = 100 // Cambiado a var, inicializado en 100 como ejemplo
)


data class Juego(
    val juegoId: Int = 0,
    val estado: String = ""  // "Pendiente", "Jugando", "Terminado" o ""
)

data class Resenna(
    val userId: String = "",
    val comentario: String = "",
    val nombreUsuario: String? = null, // Nombre opcional
    val recomendado: Boolean = false, // Indica si se recomienda o no
    val fecha: Date= Date()  // Fecha con valor predeterminado de hoy
)



