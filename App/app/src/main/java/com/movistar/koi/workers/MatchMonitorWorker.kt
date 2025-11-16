package com.movistar.koi.workers

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.movistar.koi.MainActivity
import com.movistar.koi.R
import com.movistar.koi.data.Match
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * Worker que monitorea partidos periódicamente usando WorkManager
 */
class MatchMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MatchMonitorWorker"
        private const val CHANNEL_ID = "match_alerts_channel"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 Worker de monitoreo ejecutándose...")

        return try {
            checkUpcomingMatches()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en worker: ${e.message}")
            Result.retry()
        }
    }

    /**
     * Verifica partidos próximos
     */
    private suspend fun checkUpcomingMatches() {
        val db = Firebase.firestore
        val now = Date()

        val documents = db.collection("matches")
            .whereEqualTo("status", "scheduled")
            .get()
            .await()

        Log.d(TAG, "📊 Partidos programados encontrados: ${documents.size()}")

        for (document in documents) {
            try {
                val match = document.toObject(Match::class.java)
                checkMatchForNotification(match, now)
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando partido: ${e.message}")
            }
        }
    }

    /**
     * Verifica si un partido necesita notificación
     */
    private fun checkMatchForNotification(match: Match, now: Date) {
        val matchTime = match.date
        val timeDiff = matchTime.time - now.time

        // Si el partido es en menos de 1 hora
        if (timeDiff > 0 && timeDiff <= TimeUnit.HOURS.toMillis(1)) {
            val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeDiff)
            Log.d(TAG, "⏰ Partido próximo: ${match.opponent} en $minutesLeft min")

            if (minutesLeft <= 60) {
                sendUpcomingMatchNotification(match, minutesLeft.toInt())
            }
        }

        // Si el partido debería haber empezado pero sigue como scheduled
        if (matchTime.before(now) && match.status == "scheduled") {
            Log.d(TAG, "🔴 Partido debería haber empezado: ${match.opponent}")
            sendMatchShouldStartNotification(match)
        }
    }

    /**
     * Envía notificación de partido próximo
     */
    private fun sendUpcomingMatchNotification(match: Match, minutesLeft: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timeText = when (minutesLeft) {
            in 1..10 -> "en $minutesLeft minutos"
            in 11..30 -> "en 30 minutos"
            else -> "en 1 hora"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Partido Próximo")
            .setContentText("KOI vs ${match.opponent} $timeText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El partido entre Movistar KOI y ${match.opponent} comienza $timeText. Competición: ${match.competition}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(applicationContext.getColor(R.color.koi_purple))
            .build()

        notificationManager.notify(generateNotificationId(match.id), notification)
        Log.d(TAG, "✅ Notificación enviada: ${match.opponent}")
    }

    /**
     * Envía notificación de partido que debería empezar
     */
    private fun sendMatchShouldStartNotification(match: Match) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔴 Partido por Empezar")
            .setContentText("KOI vs ${match.opponent} debería haber empezado")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El partido entre Movistar KOI y ${match.opponent} estaba programado para ahora. Competición: ${match.competition}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(applicationContext.getColor(R.color.koi_purple))
            .build()

        notificationManager.notify(generateNotificationId(match.id + "_late"), notification)
    }

    /**
     * Genera un ID único para la notificación
     */
    private fun generateNotificationId(matchId: String): Int {
        return matchId.hashCode().absoluteValue
    }
}