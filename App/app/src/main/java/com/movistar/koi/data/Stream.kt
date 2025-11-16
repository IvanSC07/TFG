package com.movistar.koi.data

/**
 * Modelo de datos para representar un stream en directo
 */
data class Stream(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val streamUrl: String = "",
    val platform: String = "", // twitch, youtube
    val isLive: Boolean = false,
    val thumbnail: String = "",
    val category: String = "" // official, matches, special
) {
    constructor() : this("", "", "", "", "", false, "", "")
}