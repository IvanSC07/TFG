package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.movistar.koi.adapters.ReactionsAdapter
import com.movistar.koi.data.News
import com.movistar.koi.databinding.FragmentNewsDetailBinding
import com.movistar.koi.services.ReactionService
import java.text.SimpleDateFormat
import java.util.Locale

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentNews: News
    private val reactionService = ReactionService()

    companion object {
        private const val TAG = "NewsDetailFragment"
        private const val ARG_NEWS = "news"

        fun newInstance(news: News): NewsDetailFragment {
            val fragment = NewsDetailFragment()
            val args = Bundle()
            args.putString(ARG_NEWS, news.title)
            args.putString("content", news.content)
            args.putString("imageUrl", news.imageUrl)
            args.putLong("date", news.date.time)
            args.putString("category", news.category)
            args.putString("id", news.id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener los argumentos
        val title = arguments?.getString(ARG_NEWS) ?: "Noticia"
        val content = arguments?.getString("content") ?: ""
        val imageUrl = arguments?.getString("imageUrl") ?: ""
        val date = arguments?.getLong("date") ?: System.currentTimeMillis()
        val category = arguments?.getString("category") ?: "general"
        val id = arguments?.getString("id") ?: ""

        // Reconstruir el objeto News
        currentNews = News(
            id = id,
            title = title,
            content = content,
            imageUrl = imageUrl,
            date = java.util.Date(date),
            category = category
        )

        setupUI()
    }

    private fun setupUI() {
        // Cargar imagen con Glide
        if (currentNews.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(currentNews.imageUrl)
                .placeholder(R.color.koi_dark_gray)
                .error(R.color.koi_light_gray)
                .centerCrop()
                .into(binding.detailNewsImage)
        }

        // Configurar categoría
        binding.detailNewsCategory.text = currentNews.category.uppercase(Locale.getDefault())

        // Configurar título
        binding.detailNewsTitle.text = currentNews.title

        // Configurar fecha formateada
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentNews.date)
        binding.detailNewsDate.text = formattedDate

        // Configurar autor
        binding.detailNewsAuthor.text = "Movistar KOI"

        // Configurar contenido
        binding.detailNewsContent.text = currentNews.content

        // Configurar reacciones en el detalle
        setupReactions()

        Log.d(TAG, "📖 Mostrando detalle de noticia: ${currentNews.title}")
    }

    private fun setupReactions() {
        val currentUserReaction = reactionService.getCurrentUserReaction(currentNews)
        val reactionsAdapter = ReactionsAdapter(
            onReactionClick = { reactionType ->
                reactionService.addReaction(
                    newsId = currentNews.id,
                    reactionType = reactionType,
                    onSuccess = {
                        // Actualizar la UI
                        setupReactions()
                    },
                    onError = { errorMessage ->
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            currentUserReaction = currentUserReaction
        )

        binding.reactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = reactionsAdapter
            setHasFixedSize(true)
        }

        // Actualizar conteos
        reactionsAdapter.updateReactionCounts(currentNews.reactions)

        // Mostrar total de reacciones
        val totalReactions = currentNews.getTotalReactions()
        binding.totalReactionsCount.text = when {
            totalReactions == 0 -> "0 reacciones"
            totalReactions == 1 -> "1 reacción"
            else -> "$totalReactions reacciones"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}