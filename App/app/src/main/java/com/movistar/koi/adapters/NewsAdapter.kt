package com.movistar.koi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.R
import com.movistar.koi.data.News
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador para mostrar la lista de noticias en un RecyclerView
 */
class NewsAdapter(
    private var newsList: List<News> = emptyList(),
    private val onItemClick: (News) -> Unit = {}
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    /**
     * ViewHolder que representa cada item de noticia
     */
    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newsImage: ImageView = itemView.findViewById(R.id.newsImage)
        private val newsCategory: TextView = itemView.findViewById(R.id.newsCategory)
        private val newsTitle: TextView = itemView.findViewById(R.id.newsTitle)
        private val newsContent: TextView = itemView.findViewById(R.id.newsContent)
        private val newsDate: TextView = itemView.findViewById(R.id.newsDate)

        /**
         * Vincula los datos de una noticia con las vistas
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

            // Configurar click listener con efecto visual
            itemView.setOnClickListener {
                // Efecto visual de click
                itemView.alpha = 0.7f
                itemView.postDelayed({
                    itemView.alpha = 1.0f
                }, 100)

                // Llamar al callback
                onItemClick(news)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount(): Int = newsList.size

    /**
     * Actualiza la lista de noticias y notifica al adapter
     */
    fun updateNews(newNewsList: List<News>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }
}