package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.movistar.koi.data.News
import com.movistar.koi.databinding.FragmentNewsDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentNews: News

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

        Log.d(TAG, "📖 Mostrando detalle de noticia: ${currentNews.title}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}