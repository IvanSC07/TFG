package com.movistar.koi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.movistar.koi.databinding.FragmentSettingsBinding

/**
 * Fragmento para configurar preferencias de notificaciones
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "SettingsFragment"
        private const val PREFS_NAME = "movistar_koi_prefs"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_MATCH_ALERTS = "match_alerts_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupClickListeners()

        setupTestButtons()
    }

    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        val matchAlertsEnabled = prefs.getBoolean(KEY_MATCH_ALERTS, true)

        binding.switchNotifications.isChecked = notificationsEnabled
        binding.switchMatchAlerts.isChecked = matchAlertsEnabled
    }

    private fun setupClickListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_NOTIFICATIONS, isChecked)

            if (isChecked) {
                Log.d(TAG, "Notificaciones activadas")
            } else {
                Log.d(TAG, "Notificaciones desactivadas")
            }
        }

        binding.switchMatchAlerts.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_MATCH_ALERTS, isChecked)

            if (isChecked) {
                Log.d(TAG, "Alertas de partidos activadas")
            } else {
                Log.d(TAG, "Alertas de partidos desactivadas")
            }
        }
    }

    private fun saveSetting(key: String, value: Boolean) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun setupTestButtons() {
        binding.btnTestNotification.setOnClickListener {
            testNotification()
        }

        binding.btnTestMatchNotification.setOnClickListener {
            testMatchNotification()
        }
    }

    /**
     * Prueba una notificación básica
     */
    private fun testNotification() {
        Log.d(TAG, "Iniciando prueba de notificación...")

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de prueba
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "test_channel",
                "Notificaciones de Prueba",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para pruebas de notificaciones"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear notificación
        val notification = NotificationCompat.Builder(requireContext(), "test_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 Movistar KOI - Prueba")
            .setContentText("¡Las notificaciones funcionan correctamente!")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Esta es una notificación de prueba. Si puedes ver este mensaje, significa que el sistema de notificaciones de la app Movistar KOI está configurado correctamente."))
            .build()

        // Mostrar notificación
        notificationManager.notify(1001, notification)

        Toast.makeText(requireContext(), "Notificación de prueba enviada", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "✅ Notificación de prueba enviada correctamente")
    }

    /**
     * Prueba una notificación de partido
     */
    private fun testMatchNotification() {
        Log.d(TAG, "Iniciando prueba de notificación de partido...")

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para partidos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "match_channel",
                "Notificaciones de Partidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre partidos y streams"
                enableLights(true)
                lightColor = ContextCompat.getColor(requireContext(), R.color.koi_purple)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir directamente a partidos
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra("open_fragment", "matches")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear notificación de partido
        val notification = NotificationCompat.Builder(requireContext(), "match_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚡ ¡Partido en Directo!")
            .setContentText("KOI vs G2 Esports - LEC")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El partido entre Movistar KOI y G2 Esports está en directo. ¡No te lo pierdas! Haz click para ver el stream."))
            .addAction(
                R.drawable.ic_notification,
                "VER STREAM",
                pendingIntent
            )
            .build()

        // Mostrar notificación
        notificationManager.notify(1002, notification)

        Toast.makeText(requireContext(), "Notificación de partido enviada", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "✅ Notificación de partido enviada correctamente")
    }
}