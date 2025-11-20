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
    val category: String = "general",
    val author: String = "Movistar KOI", // NUEVO CAMPO
    val isPublished: Boolean = true,     // NUEVO CAMPO para borrado lógico
    val tags: List<String> = emptyList() // NUEVO CAMPO para categorización
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", "", Date(), "general", "Movistar KOI", true, emptyList())
}