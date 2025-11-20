package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.movistar.koi.ManageNewsFragment
import com.movistar.koi.R
import com.movistar.koi.data.News
import com.movistar.koi.databinding.DialogNewsBinding
import java.util.*

class NewsDialog : DialogFragment() {

    private var _binding: DialogNewsBinding? = null
    private val binding get() = _binding!!
    private var existingNews: News? = null
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "NewsDialog"

        fun newInstance(news: News? = null): NewsDialog {
            val dialog = NewsDialog()
            news?.let {
                val args = Bundle()
                args.putString("id", it.id)
                args.putString("title", it.title)
                args.putString("content", it.content)
                args.putString("imageUrl", it.imageUrl)
                args.putString("category", it.category)
                args.putString("author", it.author)
                args.putStringArrayList("tags", ArrayList(it.tags))
                dialog.arguments = args
            }
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNewsBinding.inflate(LayoutInflater.from(requireContext()))

        // Configurar categorías
        setupCategories()

        // Cargar datos existentes si estamos editando
        loadExistingData()

        // Configurar listeners
        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingNews == null) "Crear Noticia" else "Editar Noticia")
            .setPositiveButton("Guardar") { _, _ -> saveNews() }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupCategories() {
        val categories = arrayOf(
            "general", "competition", "team", "player",
            "signing", "tournament", "community", "stream"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun loadExistingData() {
        arguments?.let { args ->
            existingNews = News(
                id = args.getString("id") ?: "",
                title = args.getString("title") ?: "",
                content = args.getString("content") ?: "",
                imageUrl = args.getString("imageUrl") ?: "",
                category = args.getString("category") ?: "general",
                author = args.getString("author") ?: "Movistar KOI",
                tags = args.getStringArrayList("tags") ?: emptyList()
            )

            // Llenar los campos
            binding.editTextTitle.setText(existingNews?.title)
            binding.editTextContent.setText(existingNews?.content)

            // Seleccionar categoría - CORREGIDO
            val currentCategory = existingNews?.category ?: "general"
            val adapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(currentCategory)
            if (position >= 0) {
                binding.spinnerCategory.setSelection(position)
            }

            // Cargar imagen existente
            if (!existingNews?.imageUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(existingNews?.imageUrl)
                    .placeholder(R.color.koi_light_gray)
                    .into(binding.imageViewNews)
                binding.textImageStatus.text = "Imagen cargada"
            }
        }
    }

    private fun setupListeners() {
        binding.buttonSelectImage.setOnClickListener {
            showImageUrlDialog()
        }

        binding.buttonRemoveImage.setOnClickListener {
            imageUri = null
            binding.imageViewNews.setImageResource(R.color.koi_light_gray)
            binding.textImageStatus.text = "Sin imagen"
            existingNews = existingNews?.copy(imageUrl = "") ?: News(imageUrl = "")
        }
    }

    private fun showImageUrlDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_url, null)
        val editTextUrl = dialogView.findViewById<android.widget.EditText>(R.id.editTextImageUrl)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("URL de la Imagen")
            .setPositiveButton("Cargar") { _, _ ->
                val url = editTextUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadImageFromUrl(url)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadImageFromUrl(url: String) {
        binding.textImageStatus.text = "Cargando imagen..."

        Glide.with(requireContext())
            .load(url)
            .placeholder(R.color.koi_light_gray)
            .error(R.color.koi_light_gray) // Usamos color en lugar de drawable por ahora
            .into(binding.imageViewNews)

        // Asumimos éxito después de un breve delay
        binding.imageViewNews.postDelayed({
            binding.textImageStatus.text = "Imagen cargada"
            existingNews = existingNews?.copy(imageUrl = url) ?: News(imageUrl = url)
        }, 1000)
    }

    private fun saveNews() {
        val title = binding.editTextTitle.text.toString().trim()
        val content = binding.editTextContent.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val author = "Movistar KOI"
        val imageUrl = existingNews?.imageUrl ?: ""

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(requireContext(), "Completa título y contenido", Toast.LENGTH_SHORT).show()
            return
        }

        val newsToSave = existingNews?.copy(
            title = title,
            content = content,
            category = category,
            author = author,
            imageUrl = imageUrl,
            date = if (existingNews?.id.isNullOrEmpty()) Date() else existingNews!!.date
        ) ?: News(
            title = title,
            content = content,
            category = category,
            author = author,
            imageUrl = imageUrl,
            date = Date()
        )

        if (existingNews?.id.isNullOrEmpty()) {
            createNews(newsToSave)
        } else {
            updateNews(newsToSave)
        }
    }

    private fun createNews(news: News) {
        val newDocRef = db.collection("news").document()
        val newsWithId = news.copy(id = newDocRef.id)

        newDocRef.set(newsWithId)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Noticia creada exitosamente", Toast.LENGTH_SHORT).show()
                dismiss()
                // Notificar al fragment padre para que recargue
                (parentFragment as? ManageNewsFragment)?.loadNews()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error creando noticia: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateNews(news: News) {
        db.collection("news").document(news.id)
            .set(news)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Noticia actualizada exitosamente", Toast.LENGTH_SHORT).show()
                dismiss()
                // Notificar al fragment padre para que recargue
                (parentFragment as? ManageNewsFragment)?.loadNews()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error actualizando noticia: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}