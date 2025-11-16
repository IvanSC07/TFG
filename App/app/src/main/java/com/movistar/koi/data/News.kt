package com.movistar.koi.data

import java.util.Date

/**
 * Modelo de datos para representar una noticia en la aplicación
 */
data class News(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val date: Date = Date(),
    val category: String = "general"
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", "", Date(), "general")
}