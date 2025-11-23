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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.R
import com.movistar.koi.data.Stream

/**
 * Adaptador para mostrar la lista de streams en un RecyclerView
 */
class StreamsAdapter(
    private var streamsList: List<Stream> = emptyList(),
    private val onItemClick: (Stream) -> Unit = {},  // Para acciones en vista admin
    private val isAdminView: Boolean = false  // Nuevo parámetro para diferenciar vistas
) : RecyclerView.Adapter<StreamsAdapter.StreamViewHolder>() {

    companion object {
        private const val TAG = "StreamsAdapter"
    }

    inner class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val streamThumbnail: ImageView = itemView.findViewById(R.id.streamThumbnail)
        private val streamLiveBadge: TextView = itemView.findViewById(R.id.streamLiveBadge)
        private val streamPlatformIcon: ImageView = itemView.findViewById(R.id.streamPlatformIcon)
        private val streamCategory: TextView = itemView.findViewById(R.id.streamCategory)
        private val streamTitle: TextView = itemView.findViewById(R.id.streamTitle)
        private val streamDescription: TextView = itemView.findViewById(R.id.streamDescription)
        private val btnWatchStream: Button = itemView.findViewById(R.id.btnWatchStream)

        fun bind(stream: Stream) {
            // Cargar thumbnail
            if (stream.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(stream.thumbnail)
                    .placeholder(R.color.koi_light_gray)
                    .error(R.color.koi_light_gray)
                    .centerCrop()
                    .into(streamThumbnail)
            }

            // Configurar badge EN DIRECTO
            if (stream.isLive) {
                streamLiveBadge.visibility = View.VISIBLE
                btnWatchStream.text = "VER EN DIRECTO"
                btnWatchStream.backgroundTintList = itemView.context.getColorStateList(R.color.koi_purple)
            } else {
                streamLiveBadge.visibility = View.GONE
                btnWatchStream.text = "VER CANAL"
                btnWatchStream.backgroundTintList = itemView.context.getColorStateList(R.color.koi_light_blue)
            }

            // Configurar icono de plataforma
            when (stream.platform.lowercase()) {
                "twitch" -> {
                    streamPlatformIcon.setImageResource(R.drawable.ic_twitch)
                    streamPlatformIcon.visibility = View.VISIBLE
                }
                "youtube" -> {
                    streamPlatformIcon.setImageResource(R.drawable.ic_youtube)
                    streamPlatformIcon.visibility = View.VISIBLE
                }
                else -> {
                    streamPlatformIcon.visibility = View.GONE
                }
            }

            // Configurar categoría
            streamCategory.text = when (stream.category) {
                "official" -> "OFICIAL"
                "matches" -> "PARTIDOS"
                "special" -> "ESPECIAL"
                else -> stream.category.uppercase()
            }

            // Configurar título y descripción
            streamTitle.text = stream.title
            streamDescription.text = stream.description

            // Configurar comportamiento según el tipo de vista
            if (isAdminView) {
                // En vista admin: click en item muestra acciones, botón abre stream
                itemView.setOnClickListener {
                    onItemClick(stream)  // Esto mostrará el diálogo de acciones
                }

                btnWatchStream.setOnClickListener {
                    openStream(stream.streamUrl, itemView.context)
                }
            } else {
                // En vista pública: tanto el item como el botón abren el stream
                itemView.setOnClickListener {
                    openStream(stream.streamUrl, itemView.context)
                }

                btnWatchStream.setOnClickListener {
                    openStream(stream.streamUrl, itemView.context)
                }
            }

            Log.d(TAG, "Stream bindeado: ${stream.title} - Live: ${stream.isLive} - Admin: $isAdminView")
        }

        /**
         * Abre el stream en el navegador o app nativa
         */
        private fun openStream(streamUrl: String, context: Context) {
            try {
                Log.d(TAG, "Abriendo stream: $streamUrl")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl))

                // Verificar si hay app que pueda manejar la URL
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback: abrir en navegador
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl))
                    context.startActivity(browserIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error abriendo stream: ${e.message}")
                // Podrías mostrar un Toast aquí
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        holder.bind(streamsList[position])
    }

    override fun getItemCount(): Int = streamsList.size

    fun updateStreams(newStreamsList: List<Stream>) {
        streamsList = newStreamsList
        notifyDataSetChanged()
        Log.d(TAG, "Adapter actualizado con ${streamsList.size} streams")
    }
}