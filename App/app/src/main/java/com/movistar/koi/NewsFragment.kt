package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.NewsAdapter
import com.movistar.koi.data.News
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentNewsBinding

/**
 * Fragmento para mostrar las noticias del equipo
 */
class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private val newsList = mutableListOf<News>()
    private lateinit var newsAdapter: NewsAdapter

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

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar noticias desde Firebase
        loadNews()
    }

    /**
     * Configura el RecyclerView y su adapter
     */
    private fun setupRecyclerView() {
        // Crear adapter con navegación al detalle
        newsAdapter = NewsAdapter(newsList) { news ->
            // Click listener para cada noticia - Navegar al detalle
            Log.d(TAG, "📖 Navegando al detalle de: ${news.title}")

            // Crear fragmento de detalle
            val detailFragment = NewsDetailFragment.newInstance(news)

            // Navegar al detalle usando FragmentTransaction
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment) // Reemplazar el fragmento actual
                .addToBackStack("news_list") // Permitir volver atrás
                .commit()
        }

        // Configurar RecyclerView
        binding.recyclerViewNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * Carga las noticias desde Firebase Firestore
     */
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
                        val news = document.toObject(News::class.java)
                        newsList.add(news)
                        Log.d(TAG, "📰 Noticia: ${news.title}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error convirtiendo documento: ${e.message}")
                    }
                }

                if (newsList.isNotEmpty()) {
                    // Ordenar por fecha (más reciente primero)
                    newsList.sortByDescending { it.date }

                    // Actualizar adapter
                    newsAdapter.updateNews(newsList)

                    // Mostrar RecyclerView y ocultar mensaje
                    binding.recyclerViewNews.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE

                    Log.d(TAG, "✅ ${newsList.size} noticias mostradas en RecyclerView")
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