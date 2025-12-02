package com.movistar.koi.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.R
import com.movistar.koi.data.Team

/**
 * Adaptador para mostrar la lista de equipos en un RecyclerView
 */
class TeamsAdapter(
    private var teamsList: List<Team> = emptyList(),
    private val onItemClick: (Team) -> Unit = {}
) : RecyclerView.Adapter<TeamsAdapter.TeamViewHolder>() {

    /**
     * Constantes para el adapter
     */
    companion object {
        private const val TAG = "TeamsAdapter"
    }

    /**
     * ViewHolder para mostrar un equipo
     */
    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamLogo: ImageView = itemView.findViewById(R.id.teamLogo)
        private val teamGame: TextView = itemView.findViewById(R.id.teamGame)
        private val teamName: TextView = itemView.findViewById(R.id.teamName)
        private val teamCompetition: TextView = itemView.findViewById(R.id.teamCompetition)
        private val teamCoach: TextView = itemView.findViewById(R.id.teamCoach)

        /**
         * Enlaza los datos de un equipo con la vista
         */
        fun bind(team: Team) {
            // Cargar logo del equipo
            if (team.logo.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(team.logo)
                    .placeholder(R.color.koi_light_gray)
                    .error(R.color.koi_light_gray)
                    .centerCrop()
                    .into(teamLogo)
            }

            // Configurar información del equipo
            teamGame.text = team.game
            teamName.text = team.name
            teamCompetition.text = team.competition
            teamCoach.text = "Coach: ${team.coach}"

            itemView.setOnClickListener {
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                    onItemClick(team)
                }, 100)
            }

            Log.d(TAG, "Equipo bindeado: ${team.name}")
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(view)
    }

    /**
     * Enlaza los datos de un equipo con un ViewHolder
     */
    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teamsList[position])
    }

    /**
     * Devuelve el número de elementos en la lista
     */
    override fun getItemCount(): Int = teamsList.size

    /**
     * Actualiza la lista de equipos y notifica al adaptador
     */
    fun updateTeams(newTeamsList: List<Team>) {
        teamsList = newTeamsList
        notifyDataSetChanged()
        Log.d(TAG, "Adapter actualizado con ${teamsList.size} equipos")
    }
}