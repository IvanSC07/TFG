package com.movistar.koi.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.movistar.koi.MainActivity
import com.movistar.koi.R

/**
 * Receiver para manejar notificaciones programadas de partidos
 */
class MatchNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MatchNotificationReceiver"
        private const val CHANNEL_ID = "match_alerts_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "MATCH_REMINDER" -> {
                val matchId = intent.getStringExtra("match_id")
                val opponent = intent.getStringExtra("opponent") ?: "el oponente"
                val competition = intent.getStringExtra("competition") ?: "competición"
                val minutesBefore = intent.getIntExtra("minutes_before", 60)

                Log.d(TAG, "🔔 Enviando recordatorio: $opponent en $minutesBefore min")

                sendMatchReminder(context, opponent, competition, minutesBefore)
            }
        }
    }

    /**
     * Envía un recordatorio de partido
     */
    private fun sendMatchReminder(context: Context, opponent: String, competition: String, minutesBefore: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_fragment", "matches")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = when (minutesBefore) {
            60 -> "1 hora"
            30 -> "30 minutos"
            10 -> "10 minutos"
            else -> "$minutesBefore minutos"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Partido Próximo")
            .setContentText("KOI vs $opponent en $timeText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El partido entre Movistar KOI y $opponent comienza en $timeText. Competición: $competition"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.koi_purple))
            .build()

        notificationManager.notify(generateNotificationId(), notification)
    }

    /**
     * Genera un ID único para la notificación
     */
    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }
}