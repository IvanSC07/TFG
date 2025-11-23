package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.StreamsAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Stream
import com.movistar.koi.databinding.FragmentManageStreamsBinding
import com.movistar.koi.dialogs.StreamDialog

class ManageStreamsFragment : Fragment() {

    private var _binding: FragmentManageStreamsBinding? = null
    private val binding get() = _binding!!
    private val streamsList = mutableListOf<Stream>()
    private lateinit var streamsAdapter: StreamsAdapter

    companion object {
        private const val TAG = "ManageStreamsFragment"
        private const val REQUEST_CODE_STREAM_DIALOG = 1002
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadStreams()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.title = "Gestionar Streams"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configurar botón de agregar
        binding.fabAddStream.setOnClickListener {
            showAddStreamDialog()
        }

        // Configurar RecyclerView con click listener Y indicar que es vista admin
        streamsAdapter = StreamsAdapter(
            streamsList,
            { stream -> showStreamActionsDialog(stream) },
            true
        )

        binding.recyclerViewStreams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = streamsAdapter
            setHasFixedSize(true)
        }
    }

    fun loadStreams() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.streamsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                streamsList.clear()

                for (document in documents) {
                    try {
                        // Asignar el ID del documento al stream
                        val stream = document.toObject(Stream::class.java).copy(id = document.id)
                        streamsList.add(stream)
                        Log.d(TAG, "Stream cargado: ${stream.title} - ID: ${stream.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo stream: ${e.message}")
                    }
                }

                // Ordenar: streams en directo primero
                streamsList.sortByDescending { it.isLive }

                streamsAdapter.updateStreams(streamsList)

                if (streamsList.isEmpty()) {
                    binding.statusText.text = "No hay streams programados"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.statusText.visibility = View.GONE

                    // Mostrar conteo de streams en directo
                    val liveCount = streamsList.count { it.isLive }
                    if (liveCount > 0) {
                        binding.liveCountText.text = "$liveCount en directo"
                        binding.liveCountText.visibility = View.VISIBLE
                    } else {
                        binding.liveCountText.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando streams: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando streams:", exception)
            }
    }

    private fun showAddStreamDialog() {
        val dialog = StreamDialog.newInstance()
        dialog.setTargetFragment(this, REQUEST_CODE_STREAM_DIALOG)
        dialog.show(parentFragmentManager, "stream_dialog")
    }

    private fun showStreamActionsDialog(stream: Stream) {
        val options = arrayOf("Editar", "Eliminar", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${stream.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editStream(stream)
                    1 -> deleteStream(stream)
                }
            }
            .show()
    }

    private fun editStream(stream: Stream) {
        val dialog = StreamDialog.newInstance(stream)
        dialog.setTargetFragment(this, REQUEST_CODE_STREAM_DIALOG)
        dialog.show(parentFragmentManager, "edit_stream_dialog")
    }

    private fun deleteStream(stream: Stream) {
        if (stream.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No se puede eliminar - ID inválido", Toast.LENGTH_LONG).show()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Stream")
            .setMessage("¿Estás seguro de que quieres eliminar el stream \"${stream.title}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                performDeleteStream(stream)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDeleteStream(stream: Stream) {
        if (stream.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: ID de stream inválido", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Intentando eliminar stream con ID: ${stream.id}")

        FirebaseConfig.streamsCollection
            .document(stream.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Stream eliminado", Toast.LENGTH_SHORT).show()
                loadStreams()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error eliminando stream: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error eliminando stream: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}