package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.StreamsAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Stream
import com.movistar.koi.databinding.FragmentManageStreamsBinding

class ManageStreamsFragment : Fragment() {

    private var _binding: FragmentManageStreamsBinding? = null
    private val binding get() = _binding!!
    private val streamsList = mutableListOf<Stream>()
    private lateinit var streamsAdapter: StreamsAdapter

    companion object {
        private const val TAG = "ManageStreamsFragment"
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

        // Configurar RecyclerView
        streamsAdapter = StreamsAdapter(streamsList)

        binding.recyclerViewStreams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = streamsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadStreams() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.streamsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                streamsList.clear()

                for (document in documents) {
                    try {
                        val stream = document.toObject(Stream::class.java)
                        streamsList.add(stream)
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
        android.widget.Toast.makeText(requireContext(), "Agregar stream - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}