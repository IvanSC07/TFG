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

    /**
     * Obtiene el estado legible del partido
     */
    fun getStatusText(): String {
        return when (status) {
            "scheduled" -> "Programado"
            "live" -> "En Directo"
            "finished" -> "Finalizado"
            else -> status
        }
    }

    /**
     * Verifica si el partido está en vivo
     */
    fun isLive(): Boolean = status == "live"

    /**
     * Verifica si el partido está programado
     */
    fun isScheduled(): Boolean = status == "scheduled"

    /**
     * Verifica si el partido ha finalizado
     */
    fun isFinished(): Boolean = status == "finished"
}