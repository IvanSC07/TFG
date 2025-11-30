package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.NewsAdapter
import com.movistar.koi.data.News
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentNewsBinding
import com.movistar.koi.services.ReactionService

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private val newsList = mutableListOf<News>()
    private lateinit var newsAdapter: NewsAdapter
    private val reactionService = ReactionService()

    companion object {
        private const val TAG = "NewsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(
            newsList,
            onItemClick = { news ->
                Log.d(TAG, "📖 Navegando al detalle de: ${news.title}")
                val detailFragment = NewsDetailFragment.newInstance(news)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack("news_list")
                    .commit()
            },
            onReactionClick = { reactionType, news ->
                handleReactionClick(reactionType, news)
            }
        )

        binding.recyclerViewNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
            setHasFixedSize(true)
        }
    }

    private fun handleReactionClick(reactionType: String, news: News) {
        reactionService.addReaction(
            newsId = news.id,
            reactionType = reactionType,
            onSuccess = {
                // Actualizar la lista para reflejar los cambios
                loadNews()
            },
            onError = { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadNews() {
        Log.d(TAG, "Cargando noticias desde Firebase")

        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Cargando noticias..."
        binding.recyclerViewNews.visibility = View.GONE

        FirebaseConfig.newsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                newsList.clear()

                Log.d(TAG, "✅ Firebase conectado. Documentos: ${documents.size()}")

                if (documents.isEmpty) {
                    binding.statusText.text = "No hay noticias disponibles"
                    binding.statusText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val news = document.toObject(News::class.java).copy(id = document.id)
                        newsList.add(news)
                        Log.d(TAG, "📰 Noticia: ${news.title} - Reacciones: ${news.reactions}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error convirtiendo documento: ${e.message}")
                    }
                }

                if (newsList.isNotEmpty()) {
                    newsList.sortByDescending { it.date }
                    newsAdapter.updateNews(newsList)
                    binding.recyclerViewNews.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE
                    Log.d(TAG, "✅ ${newsList.size} noticias mostradas")
                } else {
                    binding.statusText.text = "No se pudieron cargar las noticias"
                    binding.statusText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando noticias: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "❌ Error en la conexión con Firebase:", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}