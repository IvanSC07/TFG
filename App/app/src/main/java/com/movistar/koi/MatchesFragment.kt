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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.MatchesAdapter
import com.movistar.koi.data.Match
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentMatchesBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento para mostrar los partidos del equipo
 */
class MatchesFragment : Fragment() {

    private var _binding: FragmentMatchesBinding? = null
    private val binding get() = _binding!!
    private val matchesList = mutableListOf<Match>()
    private lateinit var matchesAdapter: MatchesAdapter

    companion object {
        private const val TAG = "MatchesFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMatches()
        monitorLiveMatches()
    }

    private fun setupRecyclerView() {
        matchesAdapter = MatchesAdapter(matchesList) { match ->
            Log.d(TAG, "Partido clickeado: ${match.opponent} - ${match.competition}")
            // TODO: Navegar a detalle del partido o abrir stream
        }

        binding.recyclerViewMatches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchesAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadMatches() {
        Log.d(TAG, "Cargando partidos desde Firebase")

        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Cargando partidos..."
        binding.recyclerViewMatches.visibility = View.GONE

        FirebaseConfig.matchesCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                matchesList.clear()

                Log.d(TAG, "Partidos encontrados: ${documents.size()}")

                if (documents.isEmpty) {
                    binding.statusText.text = "No hay partidos programados"
                    binding.statusText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val match = document.toObject(Match::class.java)
                        matchesList.add(match)
                        Log.d(TAG, "Partido: ${match.opponent} vs KOI - ${match.competition}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo partido: ${e.message}")
                    }
                }

                if (matchesList.isNotEmpty()) {
                    // Ordenar por fecha (más reciente primero)
                    matchesList.sortByDescending { it.date }

                    matchesAdapter.updateMatches(matchesList)

                    binding.recyclerViewMatches.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE

                    Log.d(TAG, "✅ ${matchesList.size} partidos mostrados")
                } else {
                    binding.statusText.text = "No se pudieron cargar los partidos"
                    binding.statusText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando partidos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando partidos:", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Monitorea partidos en directo y programa notificaciones
     */
    private fun monitorLiveMatches() {
        // Verificar cada minuto si hay partidos que van a empezar
        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                checkUpcomingMatches()
                handler.postDelayed(this, 60000) // Revisar cada minuto
            }
        }
        handler.postDelayed(runnable, 60000)
    }

    /**
     * Verifica partidos que van a empezar pronto
     */
    private fun checkUpcomingMatches() {
        val currentTime = Date()
        val oneHourFromNow = Date(currentTime.time + 60 * 60 * 1000) // 1 hora

        val upcomingMatches = matchesList.filter { match ->
            match.status == "scheduled" &&
                    match.date.after(currentTime) &&
                    match.date.before(oneHourFromNow)
        }

        if (upcomingMatches.isNotEmpty()) {
            Log.d(TAG, "Partidos próximos en 1 hora: ${upcomingMatches.size}")

            // Podríamos mostrar una notificación local aquí
            upcomingMatches.forEach { match ->
                showUpcomingMatchNotification(match)
            }
        }
    }

    /**
     * Muestra una notificación local para partidos próximos
     */
    private fun showUpcomingMatchNotification(match: Match) {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario (para notificaciones locales)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "upcoming_matches",
                "Partidos Próximos",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val matchTime = timeFormat.format(match.date)

        val notification = NotificationCompat.Builder(requireContext(), "upcoming_matches")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Partido próximo!")
            .setContentText("KOI vs ${match.opponent} a las $matchTime")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(match.opponent.hashCode(), notification)
    }




    /**
     * Método para probar notificaciones locales
     */
    private fun testLocalNotification() {
        Log.d(TAG, "Probando notificación local...")

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "test_channel",
                "Notificaciones de Prueba",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app al hacer click
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear notificación de prueba
        val notification = NotificationCompat.Builder(requireContext(), "test_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¡Notificación de Prueba!")
            .setContentText("Las notificaciones de Movistar KOI están funcionando correctamente")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Esta es una notificación de prueba para verificar que el sistema de notificaciones de la app Movistar KOI está funcionando correctamente. ¡Perfecto!"))
            .build()

        // Mostrar notificación
        notificationManager.notify(9999, notification)

        Log.d(TAG, "Notificación de prueba enviada")

        // Mostrar Toast para confirmación visual
        Toast.makeText(requireContext(), "Notificación de prueba enviada", Toast.LENGTH_SHORT).show()
    }
}