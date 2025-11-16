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
 * Adaptador para el calendario mensual
 */
class CalendarAdapter(
    private var matches: List<Match> = emptyList(),
    private val onDateClick: (Date) -> Unit = {},
    private val onMatchClick: (Match) -> Unit = {}
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val currentDate = Calendar.getInstance()
    private val daysInMonth = mutableListOf<Date?>()
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    companion object {
        private const val DAYS_IN_WEEK = 7
        private const val MAX_WEEKS = 6
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.dayText)
        private val matchIndicator: View = itemView.findViewById(R.id.matchIndicator)
        private val matchCountText: TextView = itemView.findViewById(R.id.matchCountText)

        fun bind(date: Date?, matchesOnDay: List<Match>) {
            if (date == null) {
                // Día vacío
                dayText.text = ""
                matchIndicator.visibility = View.GONE
                matchCountText.visibility = View.GONE
                itemView.isClickable = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
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
                // Es hoy - resaltar
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_light_blue))
                dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
                dayText.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_dark_gray))
            }

            // Mostrar indicador de partidos
            if (matchesOnDay.isNotEmpty()) {
                matchIndicator.visibility = View.VISIBLE
                if (matchesOnDay.size > 1) {
                    matchCountText.text = matchesOnDay.size.toString()
                    matchCountText.visibility = View.VISIBLE
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = daysInMonth[position]
        val matchesOnDay = getMatchesForDate(date)
        holder.bind(date, matchesOnDay)
    }

    override fun getItemCount(): Int = DAYS_IN_WEEK * MAX_WEEKS

    /**
     * Actualiza el calendario con un mes específico
     */
    fun updateCalendar(year: Int, month: Int, matches: List<Match>) {
        this.matches = matches
        this.currentDate.set(year, month, 1)
        generateDaysInMonth()
        notifyDataSetChanged()
    }

    /**
     * Obtiene el título del mes actual
     */
    fun getCurrentMonthTitle(): String {
        return monthFormat.format(currentDate.time)
    }

    /**
     * Genera los días del mes para la cuadrícula del calendario
     */
    private fun generateDaysInMonth() {
        daysInMonth.clear()

        val calendar = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Días del mes anterior (para completar la primera semana)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInPreviousMonth = getDaysInPreviousMonth(calendar)

        // Agregar días del mes anterior
        for (i in firstDayOfWeek - 2 downTo 0) {
            val prevCalendar = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, daysInPreviousMonth - i)
            }
            daysInMonth.add(prevCalendar.time)
        }

        // Días del mes actual
        val daysInCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInCurrentMonth) {
            val currentCalendar = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, i)
            }
            daysInMonth.add(currentCalendar.time)
        }

        // Días del próximo mes (para completar la cuadrícula)
        val totalCells = DAYS_IN_WEEK * MAX_WEEKS
        val remainingCells = totalCells - daysInMonth.size
        for (i in 1..remainingCells) {
            val nextCalendar = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, i)
            }
            daysInMonth.add(nextCalendar.time)
        }
    }

    private fun getDaysInPreviousMonth(calendar: Calendar): Int {
        val prevCalendar = Calendar.getInstance().apply {
            time = calendar.time
            add(Calendar.MONTH, -1)
        }
        return prevCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

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