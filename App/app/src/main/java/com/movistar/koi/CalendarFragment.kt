package com.movistar.koi

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.movistar.koi.adapters.CalendarAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Match
import com.movistar.koi.databinding.FragmentCalendarBinding
import java.util.*

/**
 * Fragmento del calendario
 */
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarAdapter: CalendarAdapter
    private val matchesList = mutableListOf<Match>()
    private val currentCalendar = Calendar.getInstance()

    /**
     * Constantes
     */
    companion object {
        private const val TAG = "CalendarFragment"
        private const val CALENDAR_PERMISSION_REQUEST = 1001
        private const val CALENDAR_WRITE_PERMISSION = Manifest.permission.WRITE_CALENDAR
    }

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupClickListeners()
        loadMatches()
    }

    /**
     * Configura el calendario
     */
    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter(
            matchesList,
            onDateClick = { date ->
                showMatchesForDate(date)
            },
            onMatchClick = { match ->
                navigateToMatchDetail(match)
            }
        )

        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }

        // Inicializar el calendario con el mes actual
        val calendar = Calendar.getInstance()
        calendarAdapter.updateCalendar(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            matchesList
        )
        updateCalendarHeader()
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupClickListeners() {
        binding.prevMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        binding.nextMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        binding.syncCalendarButton.setOnClickListener {
            syncMatchesWithCalendar()
        }

        binding.todayButton.setOnClickListener {
            currentCalendar.time = Date()
            updateCalendar()
        }
    }

    /**
     * Actualiza el calendario
     */
    private fun updateCalendar() {
        updateCalendarHeader()
        calendarAdapter.updateCalendar(
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            matchesList
        )
    }

    /**
     * Actualiza el encabezado del calendario
     */
    private fun updateCalendarHeader() {
        val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.monthYearText.text = monthFormat.format(currentCalendar.time)
    }

    /**
     * Carga los partidos
     */
    private fun loadMatches() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.matchesCollection
            .get()
            .addOnSuccessListener { documents ->
                matchesList.clear()

                for (document in documents) {
                    try {
                        val match = document.toObject(Match::class.java)
                        matchesList.add(match)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting match: ${e.message}")
                    }
                }

                updateCalendar()
                binding.progressBar.visibility = View.GONE

                if (matchesList.isEmpty()) {
                    binding.statusText.text = "No hay partidos programados"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.statusText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando partidos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error loading matches:", exception)
            }
    }

    /**
     * Muestra los partidos para una fecha
     */
    private fun showMatchesForDate(date: Date) {
        val matchesOnDate = matchesList.filter { match ->
            val matchCalendar = Calendar.getInstance().apply { time = match.date }
            val selectedCalendar = Calendar.getInstance().apply { time = date }

            matchCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                    matchCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                    matchCalendar.get(Calendar.DAY_OF_MONTH) == selectedCalendar.get(Calendar.DAY_OF_MONTH)
        }

        if (matchesOnDate.isNotEmpty()) {
            val match = matchesOnDate.first()
            navigateToMatchDetail(match)
        } else {
            Toast.makeText(requireContext(), "No hay partidos este día", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Navega a la pantalla de detalle del partido
     */
    private fun navigateToMatchDetail(match: Match) {
        Toast.makeText(requireContext(), "Partido: ${match.opponent}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Sincroniza los partidos con el calendario del dispositivo
     * NO funciona correctamente.
     */
    private fun syncMatchesWithCalendar() {
        if (!hasCalendarPermission()) {
            requestCalendarPermission()
            return
        }

        if (matchesList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay partidos para sincronizar", Toast.LENGTH_SHORT).show()
            return
        }

        var syncedCount = 0
        matchesList.forEach { match ->
            if (addMatchToCalendar(match)) {
                syncedCount++
            }
        }

        Toast.makeText(
            requireContext(),
            "$syncedCount partidos añadidos al calendario",
            Toast.LENGTH_LONG
        ).show()

        Log.d(TAG, "Sincronizados $syncedCount partidos con el calendario")
    }

    /**
     * Añade un partido al calendario del dispositivo
     * NO funciona correctamente.
     */
    private fun addMatchToCalendar(match: Match): Boolean {
        return try {
            val event = ContentValues().apply {
                put(CalendarContract.Events.TITLE, "KOI vs ${match.opponent}")
                put(CalendarContract.Events.DESCRIPTION,
                    "Partido de ${match.competition}\nEquipo: ${match.team}")
                put(CalendarContract.Events.EVENT_LOCATION, "Stream: ${match.streamUrl}")
                put(CalendarContract.Events.DTSTART, match.date.time)
                put(CalendarContract.Events.DTEND, match.date.time + 2 * 60 * 60 * 1000) // +2 horas
                put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }

            val uri: Uri? = requireContext().contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                event
            )

            uri != null
        } catch (e: Exception) {
            Log.e(TAG, "Error adding match to calendar: ${e.message}")
            false
        }
    }

    /**
     * Obtiene el ID del calendario por defecto
     */
    private fun getDefaultCalendarId(): Long {
        return try {
            val projection = arrayOf(CalendarContract.Calendars._ID)
            val cursor = requireContext().contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else {
                    1L
                }
            } ?: 1L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting default calendar ID: ${e.message}")
            1L
        }
    }

    /**
     * Comprueba si el permiso para escribir en el calendario está concedido
     */
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            CALENDAR_WRITE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita el permiso para escribir en el calendario
     */
    private fun requestCalendarPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(CALENDAR_WRITE_PERMISSION),
            CALENDAR_PERMISSION_REQUEST
        )
    }

    /**
     * Maneja la respuesta del permiso
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CALENDAR_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    syncMatchesWithCalendar()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permiso denegado. No se pueden sincronizar con el calendario",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}