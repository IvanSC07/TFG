package com.movistar.koi.data

/**
 * Clase de datos para representar una opción de administrador
 */
data class AdminOption(
    val id: Int,
    val title: String,
    val description: String,
    val iconRes: Int,
    val colorRes: Int
)