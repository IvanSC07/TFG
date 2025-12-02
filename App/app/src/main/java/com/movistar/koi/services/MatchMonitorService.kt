package com.movistar.koi.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.ListenerRegistration
import com.movistar.koi.MainActivity
import com.movistar.koi.R
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Match
import com.movistar.koi.receivers.MatchNotificationReceiver
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * Servicio para monitoreo de partidos
 */
class MatchMonitorService : Service() {

    private var matchesListener: ListenerRegistration? = null
    private var scheduledMatches = mutableSetOf<String>()
    private val alarmManager by lazy { getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    /**
     * Constantes
     */
    companion object {
        private const val TAG = "MatchMonitorService"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "match_alerts_channel"

        // Tiempos de notificación antes del partido
        private val NOTIFICATION_TIMES = listOf(60, 30, 10) // 1 hora, 30 min, 10 min

        fun startService(context: Context) {
            val intent = Intent(context, MatchMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /**
     * Inicia el servicio
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio de monitoreo de partidos iniciado")
        createNotificationChannel()
        startForegroundService()
    }

    /**
     * Maneja la ejecución del servicio
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoringMatches()
        return START_STICKY
    }

    /**
     * No se usa en este caso
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Inicia el monitoreo de partidos
     */
    private fun startMonitoringMatches() {
        Log.d(TAG, "Iniciando monitoreo de partidos...")

        matchesListener = FirebaseConfig.matchesCollection
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e(TAG, "Error escuchando partidos: ${it.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { documents ->
                    Log.d(TAG, "Partidos actualizados: ${documents.size()}")

                    for (document in documents) {
                        try {
                            val match = document.toObject(Match::class.java)
                            handleMatchUpdate(match)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error procesando partido: ${e.message}")
                        }
                    }
                }
            }
    }

    /**
     * Maneja la actualización de un partido
     */
    private fun handleMatchUpdate(match: Match) {
        when (match.status) {
            "scheduled" -> {
                // Programar notificaciones para partidos futuros
                scheduleMatchNotifications(match)
            }
            "live" -> {
                // Notificar que el partido empezó
                if (!scheduledMatches.contains(match.id)) {
                    sendLiveMatchNotification(match)
                    scheduledMatches.add(match.id)
                }
            }
            "finished" -> {
                // Limpiar partidos finalizados
                scheduledMatches.remove(match.id)
            }
        }
    }

    /**
     * Programa las notificaciones para un partido
     */
    private fun scheduleMatchNotifications(match: Match) {
        val now = Date()
        val matchTime = match.date

        // Solo programar si el partido es en el futuro
        if (matchTime.before(now)) {
            return
        }

        Log.d(TAG, "Programando notificaciones para: ${match.opponent}")

        NOTIFICATION_TIMES.forEach { minutesBefore ->
            val notificationTime = Date(matchTime.time - TimeUnit.MINUTES.toMillis(minutesBefore.toLong()))

            if (notificationTime.after(now)) {
                scheduleNotification(match, notificationTime, minutesBefore)
            }
        }
    }

    /**
     * Programa una notificación para un partido
     */
    private fun scheduleNotification(match: Match, time: Date, minutesBefore: Int) {
        val intent = Intent(this, MatchNotificationReceiver::class.java).apply {
            putExtra("match_id", match.id)
            putExtra("opponent", match.opponent)
            putExtra("competition", match.competition)
            putExtra("minutes_before", minutesBefore)
            action = "MATCH_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            generateRequestCode(match.id, minutesBefore),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.time,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                time.time,
                pendingIntent
            )
        }

        Log.d(TAG, "Notificación programada: ${match.opponent} en $minutesBefore min")
    }

    /**
     * Notifica que el partido empezó
     */
    private fun sendLiveMatchNotification(match: Match) {
        Log.d(TAG, "Notificando partido en directo: ${match.opponent}")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_fragment", "matches")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¡PARTIDO EN DIRECTO!")
            .setContentText("KOI vs ${match.opponent} - ${match.competition}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El partido entre Movistar KOI y ${match.opponent} ha comenzado en la ${match.competition}. ¡No te lo pierdas!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(getColor(R.color.koi_purple))
            .addAction(
                R.drawable.ic_notification,
                "VER PARTIDOS",
                pendingIntent
            )
            .build()

        notificationManager.notify(generateNotificationId(), notification)
    }

    /**
     * Crea el canal de notificaciones
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Partidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de partidos programados y en directo"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Inicia el servicio en primer plano
     */
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Movistar KOI")
            .setContentText("Monitoreando partidos...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Genera un código único para la solicitud
     */
    private fun generateRequestCode(matchId: String, minutesBefore: Int): Int {
        return (matchId.hashCode() + minutesBefore).absoluteValue
    }

    /**
     * Genera un ID único para la notificación
     */
    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroy() {
        super.onDestroy()
        matchesListener?.remove()
        Log.d(TAG, "Servicio de monitoreo detenido")
    }
}