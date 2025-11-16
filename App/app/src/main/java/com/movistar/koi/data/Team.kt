package com.movistar.koi.data

/**
 * Modelo de datos para representar un equipo
 */
data class Team(
    val id: String = "",
    val name: String = "",
    val game: String = "",
    val competition: String = "",
    val logo: String = "",
    val description: String = "",
    val players: List<String> = emptyList(),
    val coach: String = "",
    val achievements: List<String> = emptyList()
) {
    constructor() : this("", "", "", "", "", "", emptyList(), "", emptyList())
}