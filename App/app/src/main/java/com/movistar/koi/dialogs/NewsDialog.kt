package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.movistar.koi.ManageNewsFragment
import com.movistar.koi.R
import com.movistar.koi.data.News
import com.movistar.koi.databinding.DialogNewsBinding
import java.util.*

/**
 * Dialogo para crear o editar una noticia
 */
class NewsDialog : DialogFragment() {

    private var _binding: DialogNewsBinding? = null
    private val binding get() = _binding!!
    private var existingNews: News? = null
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Para seleccionar imagen de la galería
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.imageViewNews.setImageURI(it)
            binding.textImageStatus.text = "Imagen seleccionada - Subir al guardar"
        }
    }

    /**
     * Instancia del diálogo
     */
    companion object {
        private const val TAG = "NewsDialog"

        /**
         * Crea una nueva instancia del diálogo
         * @param news Noticia existente para editar, si es nulo se crea uno nuevo
         */
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

    /**
     * Crea el diálogo
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNewsBinding.inflate(LayoutInflater.from(requireContext()))

        // Configurar categorías
        setupCategories()

        // Cargar datos existentes si estamos editando
        loadExistingData()

        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingNews == null) "Crear Noticia" else "Editar Noticia")
            .setPositiveButton("Guardar") { _, _ ->
                binding.root.post { saveNews() }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Configura las categorías de noticias
     */
    private fun setupCategories() {
        val categories = arrayOf(
            "general", "competition", "team", "player",
            "signing", "tournament", "community", "stream"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    /**
     * Carga datos existentes de la noticia
     */
    private fun loadExistingData() {
        arguments?.let { args ->
            val newsId = args.getString("id") ?: ""

            existingNews = News(
                id = newsId,
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

            // Seleccionar categoría
            val currentCategory = existingNews?.category ?: "general"
            try {
                val adapter = binding.spinnerCategory.adapter as ArrayAdapter<*>
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i) == currentCategory) {
                        binding.spinnerCategory.setSelection(i)
                        break
                    }
                }
            } catch (e: Exception) {
                binding.spinnerCategory.setSelection(0)
            }

            // Cargar imagen existente
            if (!existingNews?.imageUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(existingNews?.imageUrl)
                    .placeholder(R.color.koi_light_gray)
                    .into(binding.imageViewNews)
                binding.textImageStatus.text = "Imagen cargada"
                imageUri = null
            }
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupListeners() {
        // Seleccionar imagen de galería
        binding.buttonSelectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Seleccionar imagen por URL
        binding.buttonSelectImageUrl.setOnClickListener {
            showImageUrlDialog()
        }

        binding.buttonRemoveImage.setOnClickListener {
            imageUri = null
            binding.imageViewNews.setImageResource(R.color.koi_light_gray)
            binding.textImageStatus.text = "Sin imagen"
            existingNews = existingNews?.copy(imageUrl = "") ?: News(imageUrl = "")
        }
    }

    /**
     * Muestra el diálogo para seleccionar la URL de la imagen
     */
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

    /**
     * Carga una imagen desde una URL
     */
    private fun loadImageFromUrl(url: String) {
        binding.textImageStatus.text = "Cargando imagen..."

        Glide.with(requireContext())
            .load(url)
            .placeholder(R.color.koi_light_gray)
            .error(R.color.koi_light_gray)
            .into(binding.imageViewNews)

        binding.imageViewNews.postDelayed({
            binding.textImageStatus.text = "Imagen cargada"
            existingNews = existingNews?.copy(imageUrl = url) ?: News(imageUrl = url)
            imageUri = null
        }, 1000)
    }

    /**
     * Guarda la noticia
     */
    private fun saveNews() {
        val title = binding.editTextTitle.text.toString().trim()
        val content = binding.editTextContent.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val author = "Movistar KOI"

        if (title.isEmpty() || content.isEmpty()) {
            showToast("Completa título y contenido")
            return
        }

        // Si hay una imagen seleccionada desde galería, subirla primero
        if (imageUri != null) {
            uploadImageAndSaveNews(title, content, category, author)
        } else {
            // Si no hay imagen nueva, guardar directamente
            saveNewsToFirestore(title, content, category, author, existingNews?.imageUrl ?: "")
        }
    }

    /**
     * Subir imagen a Firebase Storage y guardar noticia
     */
    private fun uploadImageAndSaveNews(title: String, content: String, category: String, author: String) {
        if (!isAdded || _binding == null) {
            showToast("Error: Diálogo no disponible")
            return
        }

        binding.textImageStatus.text = "Subiendo imagen..."

        val fileName = "news_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("news_images/$fileName")

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                // Obtener la URL de descarga
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    if (isAdded && _binding != null) {
                        val imageUrl = uri.toString()
                        binding.textImageStatus.text = "Imagen subida"
                        saveNewsToFirestore(title, content, category, author, imageUrl)
                    }
                }.addOnFailureListener { e ->
                    if (isAdded && _binding != null) {
                        binding.textImageStatus.text = "Error obteniendo URL"
                        showToast("Error obteniendo URL de imagen: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null) {
                    binding.textImageStatus.text = "Error subiendo imagen"
                    showToast("Error subiendo imagen: ${e.message}")

                    saveNewsToFirestore(title, content, category, author, "")
                }
            }
    }

    /**
     * Guarda la noticia en Firestore
     */
    private fun saveNewsToFirestore(title: String, content: String, category: String, author: String, imageUrl: String) {
        val isEditing = !existingNews?.id.isNullOrEmpty()

        val newsToSave = if (isEditing) {
            existingNews!!.copy(
                title = title,
                content = content,
                imageUrl = imageUrl,
                category = category,
                author = author
            )
        } else {
            News(
                title = title,
                content = content,
                imageUrl = imageUrl,
                category = category,
                author = author,
                date = Date()
            )
        }

        if (isEditing) {
            updateNews(newsToSave)
        } else {
            createNews(newsToSave)
        }
    }

    /**
     * Crea una nueva noticia
     */
    private fun createNews(news: News) {
        val newDocRef = db.collection("news").document()
        val newsWithId = news.copy(id = newDocRef.id)

        newDocRef.set(newsWithId)
            .addOnSuccessListener {
                showToast("Noticia creada exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error creando noticia: ${e.message}")
            }
    }

    /**
     * Actualiza una noticia
     */
    private fun updateNews(news: News) {
        if (news.id.isEmpty()) {
            showToast("Error: ID de noticia inválido")
            return
        }

        db.collection("news").document(news.id)
            .set(news)
            .addOnSuccessListener {
                showToast("Noticia actualizada exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error actualizando noticia: ${e.message}")
            }
    }

    /**
     * Muestra un Toast
     */
    private fun showToast(message: String) {
        try {
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NewsDialog", "Error showing toast: ${e.message}")
        }
    }

    /**
     * Cierra el diálogo
     */
    private fun dismissSafely() {
        if (isAdded) {
            dismiss()
        }
    }

    /**
     * Notifica al padre que se debe recargar la lista de noticias
     */
    private fun notifyParentToReload() {
        (targetFragment as? ManageNewsFragment)?.loadNews()
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}