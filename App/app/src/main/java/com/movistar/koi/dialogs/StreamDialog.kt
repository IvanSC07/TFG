package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.movistar.koi.ManageStreamsFragment
import com.movistar.koi.R
import com.movistar.koi.data.Stream
import com.movistar.koi.databinding.DialogStreamBinding
import java.util.*

/**
 * Dialogo para crear o editar un stream
 */
class StreamDialog : DialogFragment() {

    private var _binding: DialogStreamBinding? = null
    private val binding get() = _binding!!
    private var existingStream: Stream? = null
    private val db = FirebaseFirestore.getInstance()

    /**
     * Instancia del diálogo
     */
    companion object {
        private const val TAG = "StreamDialog"

        fun newInstance(stream: Stream? = null): StreamDialog {
            val dialog = StreamDialog()
            stream?.let {
                val args = Bundle()
                args.putString("id", it.id)
                args.putString("title", it.title)
                args.putString("description", it.description)
                args.putString("streamUrl", it.streamUrl)
                args.putString("platform", it.platform)
                args.putString("category", it.category)
                args.putString("thumbnail", it.thumbnail)
                args.putBoolean("isLive", it.isLive)
                dialog.arguments = args
            }
            return dialog
        }
    }

    /**
     * Crea el diálogo
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogStreamBinding.inflate(LayoutInflater.from(requireContext()))

        // Configurar plataformas y categorías
        setupSpinners()

        // Cargar datos existentes si estamos editando
        loadExistingData()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingStream == null) "Crear Stream" else "Editar Stream")
            .setPositiveButton("Guardar") { _, _ ->
                saveStream()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Configura las plataformas y categorías
     */
    private fun setupSpinners() {
        // Plataformas
        val platforms = arrayOf("twitch", "youtube")
        val platformAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, platforms)
        platformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlatform.adapter = platformAdapter

        // Categorías
        val categories = arrayOf("official", "matches", "special")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    /**
     * Carga datos existentes del stream
     */
    private fun loadExistingData() {
        arguments?.let { args ->
            val streamId = args.getString("id") ?: ""

            existingStream = Stream(
                id = streamId,
                title = args.getString("title") ?: "",
                description = args.getString("description") ?: "",
                streamUrl = args.getString("streamUrl") ?: "",
                platform = args.getString("platform") ?: "twitch",
                category = args.getString("category") ?: "official",
                thumbnail = args.getString("thumbnail") ?: "",
                isLive = args.getBoolean("isLive", false)
            )

            // Llenar los campos
            binding.editTextTitle.setText(existingStream?.title)
            binding.editTextDescription.setText(existingStream?.description)
            binding.editTextStreamUrl.setText(existingStream?.streamUrl)
            binding.editTextThumbnail.setText(existingStream?.thumbnail)
            binding.checkboxIsLive.isChecked = existingStream?.isLive ?: false

            // Seleccionar plataforma
            val currentPlatform = existingStream?.platform ?: "twitch"
            try {
                val adapter = binding.spinnerPlatform.adapter as ArrayAdapter<*>
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i) == currentPlatform) {
                        binding.spinnerPlatform.setSelection(i)
                        break
                    }
                }
            } catch (e: Exception) {
                binding.spinnerPlatform.setSelection(0)
            }

            // Seleccionar categoría
            val currentCategory = existingStream?.category ?: "official"
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
        }
    }

    /**
     * Guarda el stream
     */
    private fun saveStream() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val streamUrl = binding.editTextStreamUrl.text.toString().trim()
        val thumbnail = binding.editTextThumbnail.text.toString().trim()
        val platform = binding.spinnerPlatform.selectedItem.toString()
        val category = binding.spinnerCategory.selectedItem.toString()
        val isLive = binding.checkboxIsLive.isChecked

        if (title.isEmpty() || streamUrl.isEmpty()) {
            showToast("Completa título y URL del stream")
            return
        }

        // Determinar si estamos creando o editando
        val isEditing = !existingStream?.id.isNullOrEmpty()

        val streamToSave = if (isEditing) {
            existingStream!!.copy(
                title = title,
                description = description,
                streamUrl = streamUrl,
                platform = platform,
                category = category,
                thumbnail = thumbnail,
                isLive = isLive
            )
        } else {
            Stream(
                title = title,
                description = description,
                streamUrl = streamUrl,
                platform = platform,
                category = category,
                thumbnail = thumbnail,
                isLive = isLive
            )
        }

        if (isEditing) {
            updateStream(streamToSave)
        } else {
            createStream(streamToSave)
        }
    }

    /**
     * Crea un nuevo stream
     */
    private fun createStream(stream: Stream) {
        val newDocRef = db.collection("streams").document()
        val streamWithId = stream.copy(id = newDocRef.id)

        newDocRef.set(streamWithId)
            .addOnSuccessListener {
                showToast("Stream creado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error creando stream: ${e.message}")
            }
    }

    /**
     * Actualiza un stream
     */
    private fun updateStream(stream: Stream) {
        if (stream.id.isEmpty()) {
            showToast("Error: ID de stream inválido")
            return
        }

        db.collection("streams").document(stream.id)
            .set(stream)
            .addOnSuccessListener {
                showToast("Stream actualizado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error actualizando stream: ${e.message}")
            }
    }

    /**
     * Muestra un Toast
     */
    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
     * Notifica al padre que se debe recargar la lista de streams
     */
    private fun notifyParentToReload() {
        (targetFragment as? ManageStreamsFragment)?.loadStreams()
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}