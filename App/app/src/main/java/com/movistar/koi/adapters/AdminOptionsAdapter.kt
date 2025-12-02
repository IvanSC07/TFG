package com.movistar.koi.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.movistar.koi.R
import com.movistar.koi.data.AdminOption

/**
 * Adapter para mostrar opciones de administración en un RecyclerView
 */
class AdminOptionsAdapter(
    private val onItemClick: (AdminOption) -> Unit
) : RecyclerView.Adapter<AdminOptionsAdapter.AdminOptionViewHolder>() {

    private var optionsList: List<AdminOption> = emptyList()

    /**
     * ViewHolder para mostrar una opción de administración
     */
    inner class AdminOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.adminOptionIcon)
        private val title: TextView = itemView.findViewById(R.id.adminOptionTitle)
        private val description: TextView = itemView.findViewById(R.id.adminOptionDescription)
        private val cardView: View = itemView.findViewById(R.id.adminOptionCard)

        /**
         * Enlaza los datos de una opción con la vista
         */
        fun bind(option: AdminOption) {
            // Configurar icono
            icon.setImageResource(option.iconRes)
            icon.setColorFilter(ContextCompat.getColor(itemView.context, option.colorRes))

            // Configurar textos
            title.text = option.title
            description.text = option.description

            // Configurar color de fondo de la tarjeta
            cardView.setBackgroundColor(
                ContextCompat.getColor(itemView.context, option.colorRes).let { color ->
                    Color.argb(
                        30,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                    )
                }
            )

            // Click listener
            itemView.setOnClickListener {
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                    onItemClick(option)
                }, 100)
            }
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_option, parent, false)
        return AdminOptionViewHolder(view)
    }

    /**
     * Enlaza los datos de una opción con un ViewHolder
     */
    override fun onBindViewHolder(holder: AdminOptionViewHolder, position: Int) {
        holder.bind(optionsList[position])
    }

    /**
     * Devuelve el número de elementos en la lista
     */
    override fun getItemCount(): Int = optionsList.size

    /**
     * Actualiza la lista de opciones y notifica al adaptador
     */
    fun updateOptions(newOptions: List<AdminOption>) {
        optionsList = newOptions
        notifyDataSetChanged()
    }
}