package com.movistar.koi.data

import java.util.Date

/**
 * Modelo de datos para representar un partido
 */
data class Match(
    val id: String = "",
    val date: Date = Date(),
    val opponent: String = "",
    val competition: String = "",
    val result: String = "",
    val status: String = "scheduled", // scheduled, live, finished
    val team: String = "",
    val opponentLogo: String = "",
    val streamUrl: String = ""
) {
    constructor() : this("", Date(), "", "", "", "scheduled", "", "", "")
}