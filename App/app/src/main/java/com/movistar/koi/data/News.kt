package com.movistar.koi.data

import java.util.Date

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

    fun getDocumentId(): String {
        return if (id.isEmpty()) {
            "${title.hashCode()}-${date.time}"
        } else {
            id
        }
    }

    fun getTotalReactions(): Int {
        return reactions.values.sum()
    }
}