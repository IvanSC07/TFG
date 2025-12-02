package com.movistar.koi.data

import java.util.*

/**
 * Modelo de datos para eventos del calendario
 */
data class CalendarEvent(
    val id: String = "",
    val matchId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val location: String = "",
    val isAllDay: Boolean = false,
    val calendarId: Long = 0 // ID del calendario en el dispositivo
) {
    constructor() : this("", "", "", "", Date(), Date(), "", false, 0)
}