package com.movistar.koi.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.R
import com.movistar.koi.data.Match
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador para mostrar la lista de partidos en un RecyclerView
 */
class MatchesAdapter(
    private var matchesList: List<Match> = emptyList(),
    private val onItemClick: (Match) -> Unit = {}
) : RecyclerView.Adapter<MatchesAdapter.MatchViewHolder>() {

    companion object {
        private const val TAG = "MatchesAdapter"
    }

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val competition: TextView = itemView.findViewById(R.id.matchCompetition)
        private val koiLogo: ImageView = itemView.findViewById(R.id.koiLogo)
        private val opponentLogo: ImageView = itemView.findViewById(R.id.opponentLogo)
        private val opponentName: TextView = itemView.findViewById(R.id.opponentName)
        private val matchStatus: TextView = itemView.findViewById(R.id.matchStatus)
        private val matchDate: TextView = itemView.findViewById(R.id.matchDate)
        private val matchTeam: TextView = itemView.findViewById(R.id.matchTeam)
        private val watchStreamButton: Button = itemView.findViewById(R.id.watchStreamButton)

        /**
         * Vincula los datos de un partido con las vistas
         */
        fun bind(match: Match) {
            // Configurar competición
            competition.text = match.competition

            koiLogo.setImageResource(R.drawable.logosinfondo)

            // Cargar logo del oponente
            if (match.opponentLogo.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(match.opponentLogo)
                    .placeholder(R.color.koi_light_gray)
                    .error(R.color.koi_light_gray)
                    .centerInside()
                    .into(opponentLogo)
            } else {
                // Si no hay logo, mostrar placeholder
                opponentLogo.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_light_gray))
            }

            // Configurar nombre del oponente
            opponentName.text = match.opponent

            // Configurar equipo
            matchTeam.text = when (match.team) {
                "VALORANT" -> "VALORANT"
                "League of Legends" -> "LoL"
                else -> match.team
            }

            // Configurar fecha y hora
            val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(match.date)
            matchDate.text = formattedDate

            // Configurar estado/resultado con colores diferentes
            when (match.status) {
                "scheduled" -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    matchStatus.text = timeFormat.format(match.date)
                    matchStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_light_blue))
                    matchStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
                }
                "live" -> {
                    matchStatus.text = "EN DIRECTO"
                    matchStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                    matchStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
                }
                "finished" -> {
                    matchStatus.text = match.result.ifEmpty { "FIN" }
                    matchStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_purple))
                    matchStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
                }
                else -> {
                    matchStatus.text = match.status
                    matchStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_dark_gray))
                    matchStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
                }
            }

            // Configurar botón de ver stream
            if (match.status == "live" && match.streamUrl.isNotEmpty()) {
                watchStreamButton.visibility = View.VISIBLE
                watchStreamButton.setOnClickListener {
                    Log.d(TAG, "Intentando abrir stream: ${match.streamUrl}")
                    openStream(match.streamUrl, itemView.context)
                }
            } else {
                watchStreamButton.visibility = View.GONE
            }

            // Click listener para el item completo
            itemView.setOnClickListener {
                // Efecto visual de click
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                    onItemClick(match)
                }, 100)
            }

            // Log para debugging
            Log.d(TAG, "Partido bindeado: ${match.opponent} - ${match.status} - Stream: ${match.streamUrl}")
        }

        /**
         * Abre el stream en el navegador o app de Twitch
         */
        private fun openStream(streamUrl: String, context: Context) {
            try {
                Log.d(TAG, "Abriendo stream URL: $streamUrl")

                // Intent para abrir en app de Twitch si está instalada
                val twitchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl))
                twitchIntent.setPackage("tv.twitch.android.app")

                // Verificar si la app de Twitch está instalada
                val isTwitchInstalled = try {
                    context.packageManager.getPackageInfo("tv.twitch.android.app", 0)
                    true
                } catch (e: Exception) {
                    false
                }

                if (isTwitchInstalled) {
                    Log.d(TAG, "Twitch app encontrada, abriendo en app")
                    context.startActivity(twitchIntent)
                } else {
                    // Si no está instalada, abrir en navegador
                    Log.d(TAG, "Twitch app no encontrada, abriendo en navegador")
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl))
                    context.startActivity(browserIntent)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error abriendo stream: ${e.message}", e)

                // Fallback: intentar abrir en navegador
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl))
                    context.startActivity(browserIntent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error incluso con fallback: ${e2.message}")
                    // Podrías mostrar un Toast al usuario aquí
                }
            }
        }
    }

    /**
     * Crea nuevas vistas (invocado por el layout manager)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    /**
     * Reemplaza el contenido de una vista (invocado por el layout manager)
     */
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        try {
            holder.bind(matchesList[position])
        } catch (e: Exception) {
            Log.e(TAG, "Error en onBindViewHolder para posición $position: ${e.message}", e)
        }
    }

    /**
     * Retorna el tamaño del dataset (invocado por el layout manager)
     */
    override fun getItemCount(): Int {
        return matchesList.size
    }

    /**
     * Actualiza la lista de partidos y notifica al adapter
     */
    fun updateMatches(newMatchesList: List<Match>) {
        matchesList = newMatchesList
        Log.d(TAG, "Adapter actualizado con ${matchesList.size} partidos")
        notifyDataSetChanged()
    }

    /**
     * Filtra partidos por estado
     */
    fun filterByStatus(status: String) {
        val filteredList = if (status == "all") {
            matchesList
        } else {
            matchesList.filter { it.status == status }
        }
        updateMatches(filteredList)
    }

    /**
     * Filtra partidos por equipo
     */
    fun filterByTeam(team: String) {
        val filteredList = if (team == "all") {
            matchesList
        } else {
            matchesList.filter { it.team == team }
        }
        updateMatches(filteredList)
    }

    /**
     * Obtiene partidos en directo
     */
    fun getLiveMatches(): List<Match> {
        return matchesList.filter { it.status == "live" }
    }

    /**
     * Obtiene próximos partidos
     */
    fun getUpcomingMatches(): List<Match> {
        return matchesList.filter { it.status == "scheduled" }
    }

    /**
     * Obtiene partidos finalizados
     */
    fun getFinishedMatches(): List<Match> {
        return matchesList.filter { it.status == "finished" }
    }
}