package com.movistar.koi.data

import java.util.Date

/**
 * Modelo de datos para representar una noticia
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
    val tags: List<String> = emptyList(),
    val reactions: Map<String, Int> = emptyMap(),
    val userReactions: Map<String, String> = emptyMap()
) {
    constructor() : this("", "", "", "", Date(), "general", "Movistar KOI", true, emptyList(), emptyMap(), emptyMap())

    /**
     * Obtiene el ID del documento para Firestore
     */
    fun getDocumentId(): String {
        return if (id.isEmpty()) {
            "${title.hashCode()}-${date.time}"
        } else {
            id
        }
    }

}