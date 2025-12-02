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
import com.movistar.koi.StreamPlayerActivity
import com.movistar.koi.data.Stream

/**
 * Adaptador para mostrar los streams en un RecyclerView
 */
class StreamsAdapter(
    private var streamsList: List<Stream> = emptyList(),
    private val onItemClick: (Stream) -> Unit = {},  // Para acciones en vista admin
    private val isAdminView: Boolean = false  // Nuevo parámetro para diferenciar vistas
) : RecyclerView.Adapter<StreamsAdapter.StreamViewHolder>() {

    /**
     * Constantes para el adapter
     */
    companion object {
        private const val TAG = "StreamsAdapter"
    }

    /**
     * ViewHolder para mostrar un stream
     */
    inner class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val streamThumbnail: ImageView = itemView.findViewById(R.id.streamThumbnail)
        private val streamLiveBadge: TextView = itemView.findViewById(R.id.streamLiveBadge)
        private val streamPlatformIcon: ImageView = itemView.findViewById(R.id.streamPlatformIcon)
        private val streamCategory: TextView = itemView.findViewById(R.id.streamCategory)
        private val streamTitle: TextView = itemView.findViewById(R.id.streamTitle)
        private val streamDescription: TextView = itemView.findViewById(R.id.streamDescription)
        private val btnWatchStream: Button = itemView.findViewById(R.id.btnWatchStream)

        /**
         * Enlaza los datos de un stream con la vista
         */
        fun bind(stream: Stream) {
            if (stream.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(stream.thumbnail)
                    .placeholder(R.color.koi_light_gray)
                    .error(R.color.koi_light_gray)
                    .centerCrop()
                    .into(streamThumbnail)
            }

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
                itemView.setOnClickListener {
                    onItemClick(stream)
                }

                btnWatchStream.setOnClickListener {
                    openStream(stream, itemView.context)
                }
            } else {
                itemView.setOnClickListener {
                    openStream(stream, itemView.context)
                }

                btnWatchStream.setOnClickListener {
                    openStream(stream, itemView.context)
                }
            }

            Log.d(TAG, "Stream bindeado: ${stream.title} - Live: ${stream.isLive} - Admin: $isAdminView")
        }

        /**
         * Abre el stream en la app de la plataforma o en el navegador
         */
        private fun openStream(stream: Stream, context: Context) {
            try {
                // Para Twitch y YouTube, usar el reproductor interno
                if (stream.platform.lowercase() in listOf("twitch", "youtube")) {
                    val intent = Intent(context, StreamPlayerActivity::class.java).apply {
                        putExtra(StreamPlayerActivity.EXTRA_STREAM_URL, stream.streamUrl)
                        putExtra(StreamPlayerActivity.EXTRA_PLATFORM, stream.platform)
                        putExtra(StreamPlayerActivity.EXTRA_STREAM_TITLE, stream.title)
                    }
                    context.startActivity(intent)
                } else {
                    // Para otras plataformas, usar intent normal
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.streamUrl))
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.streamUrl))
                        context.startActivity(browserIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error abriendo stream: ${e.message}")
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.streamUrl))
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error en fallback: ${e2.message}")
                }
            }
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }

    /**
     * Enlaza los datos de un stream con un ViewHolder
     */
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        holder.bind(streamsList[position])
    }

    /**
     * Devuelve el número de elementos en la lista
     */
    override fun getItemCount(): Int = streamsList.size

    /**
     * Actualiza la lista de streams y notifica al adaptador
     */
    fun updateStreams(newStreamsList: List<Stream>) {
        streamsList = newStreamsList
        notifyDataSetChanged()
        Log.d(TAG, "Adapter actualizado con ${streamsList.size} streams")
    }
}