package com.movistar.koi.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.movistar.koi.MainActivity
import com.movistar.koi.R

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "movistar_koi_channel"
        private const val CHANNEL_NAME = "Movistar KOI Notifications"
    }

    /**
     * Se llama cuando se recibe un nuevo token FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        // Aquí podrías enviar el token a tu servidor si es necesario
    }

    /**
     * Se llama cuando se recibe un mensaje FCM
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Mensaje recibido: ${remoteMessage.data}")

        // Verificar si el mensaje contiene datos de notificación
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Movistar KOI"
            val body = notification.body ?: "Nueva actualización"

            // Mostrar la notificación
            sendNotification(title, body)
        }

        // También puedes manejar datos personalizados
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Datos del mensaje: ${remoteMessage.data}")

            val matchId = remoteMessage.data["match_id"]
            val type = remoteMessage.data["type"]

            if (type == "match_starting" && matchId != null) {
                sendMatchNotification(matchId, remoteMessage.data)
            }
        }
    }

    /**
     * Crea y muestra una notificación básica
     */
    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear el canal de notificación (requerido en Android 8+)
        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Necesitarás crear este icono
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Crea una notificación específica para partidos
     */
    private fun sendMatchNotification(matchId: String, data: Map<String, String>) {
        val opponent = data["opponent"] ?: "el oponente"
        val competition = data["competition"] ?: "competición"

        val title = "🏆 Partido en directo!"
        val body = "KOI vs $opponent - $competition"

        sendNotification(title, body)
    }

    /**
     * Crea el canal de notificación (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de Movistar KOI"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}