package com.movistar.koi.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.movistar.koi.R
import com.movistar.koi.data.Match
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para mostrar los días del calendario en un RecyclerView
 */
class CalendarAdapter(
    private var matches: List<Match> = emptyList(),
    private val onDateClick: (Date) -> Unit = {},
    private val onMatchClick: (Match) -> Unit = {}
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val currentDate = Calendar.getInstance()
    private val daysInMonth = mutableListOf<Date?>() // Inicializar la lista
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    /**
     * Constantes para el calendario
     */
    companion object {
        private const val DAYS_IN_WEEK = 7
        private const val MAX_WEEKS = 6
        private const val TOTAL_DAYS = DAYS_IN_WEEK * MAX_WEEKS // 42 días
    }

    /**
     * ViewHolder para mostrar un día del calendario
     */
    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.dayText)
        private val matchIndicator: View = itemView.findViewById(R.id.matchIndicator)
        private val matchCountText: TextView = itemView.findViewById(R.id.matchCountText)

        /**
         * Enlaza los datos de un día con la vista
         */
        fun bind(date: Date?, matchesOnDay: List<Match>) {
            if (date == null) {
                // Día vacío
                dayText.text = ""
                matchIndicator.visibility = View.GONE
                matchCountText.visibility = View.GONE
                itemView.isClickable = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
                dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.calendar_day_text_secondary))
                return
            }

            // Configurar día
            dayText.text = dateFormat.format(date)

            // Verificar si es hoy
            val today = Calendar.getInstance()
            val cellDate = Calendar.getInstance().apply { time = date }

            if (today.get(Calendar.YEAR) == cellDate.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == cellDate.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == cellDate.get(Calendar.DAY_OF_MONTH)) {
                itemView.setBackgroundResource(R.drawable.calendar_day_background)
                itemView.isSelected = true
                dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.calendar_day_text_today))
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
                itemView.isSelected = false
                // Verificar si es del mes actual o de meses adyacentes
                val currentCalendar = Calendar.getInstance().apply { time = currentDate.time }
                if (cellDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                    dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.calendar_day_text))
                } else {
                    dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.calendar_day_text_secondary))
                }
            }

            // Mostrar indicador de partidos
            if (matchesOnDay.isNotEmpty()) {
                matchIndicator.visibility = View.VISIBLE
                if (matchesOnDay.size > 1) {
                    matchCountText.text = matchesOnDay.size.toString()
                    matchCountText.visibility = View.VISIBLE
                    matchCountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.calendar_match_count_text))
                } else {
                    matchCountText.visibility = View.GONE
                }
            } else {
                matchIndicator.visibility = View.GONE
                matchCountText.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                onDateClick(date)
            }

            // Click largo para ver partidos del día
            itemView.setOnLongClickListener {
                if (matchesOnDay.isNotEmpty()) {
                    onMatchClick(matchesOnDay.first())
                }
                true
            }
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    /**
     * Enlaza los datos de un día con un ViewHolder
     */
    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        // Verificar que la posición es válida
        if (position < daysInMonth.size) {
            val date = daysInMonth[position]
            val matchesOnDay = getMatchesForDate(date)
            holder.bind(date, matchesOnDay)
        } else {
            // Si la posición no es válida, mostrar día vacío
            holder.bind(null, emptyList())
        }
    }

    override fun getItemCount(): Int = TOTAL_DAYS // Siempre 42 días (6 semanas)

    /**
     * Actualiza la lista de partidos y notifica al adaptador
     */
    fun updateCalendar(year: Int, month: Int, matches: List<Match>) {
        this.matches = matches
        this.currentDate.set(year, month, 1)
        generateDaysInMonth()
        notifyDataSetChanged()
    }

    /**
     * Devuelve el título del mes actual
     */
    fun getCurrentMonthTitle(): String {
        return monthFormat.format(currentDate.time)
    }

    /**
     * Genera la lista de días del mes actual
     */
    private fun generateDaysInMonth() {
        daysInMonth.clear()

        val calendar = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val firstDayOffset = when (firstDayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> firstDayOfWeek - 2
        }

        val daysInPreviousMonth = getDaysInPreviousMonth(calendar)
        for (i in firstDayOffset downTo 0) {
            val prevCalendar = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, daysInPreviousMonth - i)
            }
            daysInMonth.add(prevCalendar.time)
        }

        val daysInCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInCurrentMonth) {
            val currentCalendar = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, i)
            }
            daysInMonth.add(currentCalendar.time)
        }

        val remainingCells = TOTAL_DAYS - daysInMonth.size
        for (i in 1..remainingCells) {
            val nextCalendar = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, i)
            }
            daysInMonth.add(nextCalendar.time)
        }

        while (daysInMonth.size < TOTAL_DAYS) {
            daysInMonth.add(null)
        }
    }

    /**
     * Devuelve el número de días en el mes anterior
     */
    private fun getDaysInPreviousMonth(calendar: Calendar): Int {
        val prevCalendar = Calendar.getInstance().apply {
            time = calendar.time
            add(Calendar.MONTH, -1)
        }
        return prevCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Devuelve la lista de partidos para un día específico
     */
    private fun getMatchesForDate(date: Date?): List<Match> {
        if (date == null) return emptyList()

        val calendar = Calendar.getInstance().apply { time = date }
        return matches.filter { match ->
            val matchCalendar = Calendar.getInstance().apply { time = match.date }
            matchCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    matchCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                    matchCalendar.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)
        }
    }
}