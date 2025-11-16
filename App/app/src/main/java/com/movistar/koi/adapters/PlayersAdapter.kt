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
import com.movistar.koi.data.Player

/**
 * Adaptador para mostrar la lista de jugadores en un RecyclerView
 */
class PlayersAdapter(
    private var playersList: List<Player> = emptyList(),
    private val onItemClick: (Player) -> Unit = {}  // ← Callback para navegación
) : RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

    companion object {
        private const val TAG = "PlayersAdapter"
    }

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerPhoto: ImageView = itemView.findViewById(R.id.playerPhoto)
        private val playerNickname: TextView = itemView.findViewById(R.id.playerNickname)
        private val playerName: TextView = itemView.findViewById(R.id.playerName)
        private val playerRole: TextView = itemView.findViewById(R.id.playerRole)
        private val playerNationality: TextView = itemView.findViewById(R.id.playerNationality)
        private val playerAge: TextView = itemView.findViewById(R.id.playerAge)

        fun bind(player: Player) {
            // Cargar foto del jugador
            if (player.photo.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(player.photo)
                    .placeholder(R.color.koi_light_gray)
                    .error(R.color.koi_light_gray)
                    .centerCrop()
                    .into(playerPhoto)
            }

            // Configurar información del jugador
            playerNickname.text = player.nickname
            playerName.text = player.name
            playerRole.text = player.role
            playerNationality.text = getFlagEmoji(player.nationality) + " " + player.nationality.take(3).uppercase()
            playerAge.text = player.age.toString()

            // Click listener - Ahora navega al detalle
            itemView.setOnClickListener {
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                    onItemClick(player)  // ← Ejecuta el callback de navegación
                }, 100)
            }

            Log.d(TAG, "Jugador bindeado: ${player.nickname}")
        }

        /**
         * Obtiene emoji de bandera basado en la nacionalidad
         */
        private fun getFlagEmoji(nationality: String): String {
            return when (nationality.lowercase()) {
                "españa", "spain" -> "🇪🇸"
                "francia", "france" -> "🇫🇷"
                "italia", "italy" -> "🇮🇹"
                "alemania", "germany" -> "🇩🇪"
                "reino unido", "uk" -> "🇬🇧"
                "estados unidos", "usa" -> "🇺🇸"
                else -> "🏴"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(playersList[position])
    }

    override fun getItemCount(): Int = playersList.size

    fun updatePlayers(newPlayersList: List<Player>) {
        playersList = newPlayersList
        notifyDataSetChanged()
        Log.d(TAG, "Adapter actualizado con ${playersList.size} jugadores")
    }
}