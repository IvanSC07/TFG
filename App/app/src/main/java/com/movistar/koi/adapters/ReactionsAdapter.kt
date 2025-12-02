package com.movistar.koi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.movistar.koi.R
import com.movistar.koi.data.ReactionItem

/**
 * Adaptador para mostrar las reacciones en un RecyclerView
 */
class ReactionsAdapter(
    private val onReactionClick: (String) -> Unit,
    private val currentUserReaction: String? = null
) : RecyclerView.Adapter<ReactionsAdapter.ReactionViewHolder>() {

    /**
     * Lista de reacciones disponibles
     */
    private val reactions = listOf(
        ReactionItem("like", "👍", "Me gusta"),
        ReactionItem("heart", "❤️", "Me encanta"),
        ReactionItem("laugh", "😄", "Divertido"),
        ReactionItem("wow", "😲", "Sorprendido"),
        ReactionItem("sad", "😢", "Triste"),
        ReactionItem("angry", "😠", "Enojado"),
        ReactionItem("heart_eyes", "😍", "Corazón en los ojos")
    )

    /**
     * Conteo de reacciones por tipo
     */
    private var reactionCounts: Map<String, Int> = emptyMap()

    /**
     * ViewHolder para mostrar una reacción
     */
    inner class ReactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reactionEmoji: TextView = itemView.findViewById(R.id.reactionEmoji)
        private val reactionCount: TextView = itemView.findViewById(R.id.reactionCount)

        /**
         * Enlaza los datos de una reacción con la vista
         */
        fun bind(reaction: ReactionItem, count: Int, isSelected: Boolean) {
            reactionEmoji.text = reaction.emoji
            reactionCount.text = if (count > 0) count.toString() else ""

            // Resaltar si es la reacción del usuario actual
            if (isSelected) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.koi_purple))
                reactionCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_white))
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
                reactionCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.koi_light_gray))
            }

            itemView.setOnClickListener {
                onReactionClick(reaction.type)
            }
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reaction, parent, false)
        return ReactionViewHolder(view)
    }

    /**
     * Enlaza los datos de una reacción con un ViewHolder
     */
    override fun onBindViewHolder(holder: ReactionViewHolder, position: Int) {
        val reaction = reactions[position]
        val count = reactionCounts[reaction.type] ?: 0
        val isSelected = currentUserReaction == reaction.type
        holder.bind(reaction, count, isSelected)
    }

    /**
     * Devuelve el número de elementos en la lista
     */
    override fun getItemCount(): Int = reactions.size

    /**
     * Actualiza el conteo de reacciones y notifica al adaptador
     */
    fun updateReactionCounts(counts: Map<String, Int>) {
        reactionCounts = counts
        notifyDataSetChanged()
    }
}