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
    val author: String = "Movistar KOI",
    val isPublished: Boolean = true,
    val tags: List<String> = emptyList()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", "", Date(), "general", "Movistar KOI", true, emptyList())

    /**
     * Obtiene el ID del documento de Firestore
     */
    fun getDocumentId(): String {
        return if (id.isEmpty()) {
            // Si no tenemos ID, generamos uno basado en título y fecha
            "${title.hashCode()}-${date.time}"
        } else {
            id
        }
    }
}