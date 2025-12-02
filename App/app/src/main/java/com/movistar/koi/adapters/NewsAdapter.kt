package com.movistar.koi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.R
import com.movistar.koi.data.News
import com.movistar.koi.services.ReactionService
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para mostrar noticias en un RecyclerView
 */
class NewsAdapter(
    private var newsList: List<News> = emptyList(),
    private val onItemClick: (News) -> Unit = {},
    private val onReactionClick: (String, News) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private val reactionService = ReactionService()

    /**
     * ViewHolder para mostrar una noticia
     */
    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newsImage: ImageView = itemView.findViewById(R.id.newsImage)
        private val newsCategory: TextView = itemView.findViewById(R.id.newsCategory)
        private val newsTitle: TextView = itemView.findViewById(R.id.newsTitle)
        private val newsContent: TextView = itemView.findViewById(R.id.newsContent)
        private val newsDate: TextView = itemView.findViewById(R.id.newsDate)
        private val reactionsRecyclerView: RecyclerView = itemView.findViewById(R.id.reactionsRecyclerView)

        /**
         * Enlaza los datos de la noticia con la vista
         */
        fun bind(news: News) {
            // Cargar imagen con Glide
            if (news.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(news.imageUrl)
                    .placeholder(R.color.koi_dark_gray)
                    .error(R.color.koi_light_gray)
                    .centerCrop()
                    .into(newsImage)
            } else {
                newsImage.setBackgroundColor(itemView.context.getColor(R.color.koi_light_gray))
            }

            // Configurar categoría
            newsCategory.text = news.category.uppercase(Locale.getDefault())

            // Configurar título y contenido
            newsTitle.text = news.title
            newsContent.text = news.content

            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(news.date)
            newsDate.text = formattedDate

            // Configurar reacciones
            setupReactions(news)

            // Configurar click listener para la noticia completa
            itemView.setOnClickListener {
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                }, 100)
                onItemClick(news)
            }
        }

        /**
         * Configura el RecyclerView de reacciones para la noticia
         */
        private fun setupReactions(news: News) {
            val currentUserReaction = reactionService.getCurrentUserReaction(news)
            val reactionsAdapter = ReactionsAdapter(
                onReactionClick = { reactionType ->
                    onReactionClick(reactionType, news)
                },
                currentUserReaction = currentUserReaction
            )

            reactionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = reactionsAdapter
                setHasFixedSize(true)
            }

            // Actualizar conteos
            reactionsAdapter.updateReactionCounts(news.reactions)
        }
    }

    /**
     * Crea un nuevo ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    /**
     * Enlaza los datos de una noticia con un ViewHolder
     */
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    /**
     * Devuelve el número de elementos en la lista
     */
    override fun getItemCount(): Int = newsList.size

    /**
     * Actualiza la lista de noticias y notifica al adaptador
     */
    fun updateNews(newNewsList: List<News>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}