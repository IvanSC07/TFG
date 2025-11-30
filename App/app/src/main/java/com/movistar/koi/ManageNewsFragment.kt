package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.movistar.koi.adapters.NewsAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.News
import com.movistar.koi.databinding.FragmentManageNewsBinding
import com.movistar.koi.dialogs.NewsDialog
import com.movistar.koi.services.ReactionService

class ManageNewsFragment : Fragment() {

    private var _binding: FragmentManageNewsBinding? = null
    private val binding get() = _binding!!
    private val newsList = mutableListOf<News>()
    private lateinit var newsAdapter: NewsAdapter
    private val reactionService = ReactionService()

    companion object {
        private const val TAG = "ManageNewsFragment"
        private const val REQUEST_CODE_NEWS_DIALOG = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadNews()
    }

    private fun setupUI() {
        binding.toolbar.title = "Gestionar Noticias"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.fabAddNews.setOnClickListener {
            showAddNewsDialog()
        }

        // CORREGIDO: Ahora pasamos los 3 parámetros requeridos
        newsAdapter = NewsAdapter(
            newsList = newsList,
            onItemClick = { news ->
                showNewsActionsDialog(news)
            },
            onReactionClick = { reactionType, news ->
                // En el modo administración, podemos deshabilitar las reacciones o manejarlas diferente
                Toast.makeText(requireContext(), "Modo administración: Las reacciones están deshabilitadas", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
            setHasFixedSize(true)
        }
    }

    fun loadNews() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewNews.visibility = View.GONE
        binding.statusText.visibility = View.GONE

        FirebaseConfig.newsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                newsList.clear()

                for (document in documents) {
                    try {
                        val news = document.toObject(News::class.java).copy(id = document.id)
                        newsList.add(news)
                        Log.d(TAG, "Noticia cargada: ${news.title} - ID: ${news.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo noticia: ${e.message}")
                    }
                }

                newsAdapter.updateNews(newsList)

                if (newsList.isEmpty()) {
                    binding.statusText.text = "No hay noticias publicadas"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewNews.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando noticias: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando noticias:", exception)
            }
    }

    private fun showAddNewsDialog() {
        val dialog = NewsDialog.newInstance()
        dialog.setTargetFragment(this, REQUEST_CODE_NEWS_DIALOG)
        dialog.show(parentFragmentManager, "news_dialog")
    }

    private fun showNewsActionsDialog(news: News) {
        val options = arrayOf("Editar", "Eliminar", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${news.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editNews(news)
                    1 -> deleteNews(news)
                }
            }
            .show()
    }

    private fun editNews(news: News) {
        val dialog = NewsDialog.newInstance(news)
        dialog.setTargetFragment(this, REQUEST_CODE_NEWS_DIALOG)
        dialog.show(parentFragmentManager, "edit_news_dialog")
    }

    private fun deleteNews(news: News) {
        if (news.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No se puede eliminar - ID inválido", Toast.LENGTH_LONG).show()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Noticia")
            .setMessage("¿Estás seguro de que quieres eliminar la noticia \"${news.title}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                performDeleteNews(news)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDeleteNews(news: News) {
        if (news.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: ID de noticia inválido", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Intentando eliminar noticia con ID: ${news.id}")

        FirebaseConfig.newsCollection
            .document(news.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Noticia eliminada", Toast.LENGTH_SHORT).show()
                loadNews()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error eliminando noticia: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error eliminando noticia: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}