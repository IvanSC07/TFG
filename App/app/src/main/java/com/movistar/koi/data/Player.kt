package com.movistar.koi.data

/**
 * Modelo de datos para representar un jugador
 */
data class Player(
    val id: String = "",
    val name: String = "",
    val nickname: String = "",
    val role: String = "",
    val team: String = "",
    val photo: String = "",
    val nationality: String = "",
    val age: Int = 0,
    val bio: String = ""
) {
    constructor() : this("", "", "", "", "", "", "", 0, "")
}