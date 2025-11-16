package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.StreamsAdapter
import com.movistar.koi.data.Stream
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentStreamBinding

/**
 * Fragmento para mostrar los streams en directo de Movistar KOI
 */
class StreamFragment : Fragment() {

    private var _binding: FragmentStreamBinding? = null
    private val binding get() = _binding!!
    private val streamsList = mutableListOf<Stream>()
    private lateinit var streamsAdapter: StreamsAdapter

    companion object {
        private const val TAG = "StreamFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadStreams()
    }

    private fun setupRecyclerView() {
        streamsAdapter = StreamsAdapter(streamsList)

        binding.recyclerViewStreams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = streamsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadStreams() {
        Log.d(TAG, "Cargando streams desde Firebase")

        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Cargando streams..."
        binding.recyclerViewStreams.visibility = View.GONE

        FirebaseConfig.streamsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                streamsList.clear()

                Log.d(TAG, "📺 Streams encontrados: ${documents.size()}")

                if (documents.isEmpty()) {
                    binding.statusText.text = "No hay streams disponibles"
                    binding.statusText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val stream = document.toObject(Stream::class.java)
                        streamsList.add(stream)
                        Log.d(TAG, "🎥 Stream: ${stream.title} - Live: ${stream.isLive}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error convirtiendo stream: ${e.message}")
                    }
                }

                if (streamsList.isNotEmpty()) {
                    // Ordenar: streams en directo primero
                    streamsList.sortByDescending { it.isLive }

                    streamsAdapter.updateStreams(streamsList)

                    binding.recyclerViewStreams.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE

                    Log.d(TAG, "✅ ${streamsList.size} streams mostrados")

                    // Mostrar conteo de streams en directo
                    val liveCount = streamsList.count { it.isLive }
                    if (liveCount > 0) {
                        binding.statusText.text = "$liveCount streams en directo"
                        binding.statusText.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
                        binding.statusText.visibility = View.VISIBLE
                    }
                } else {
                    binding.statusText.text = "No se pudieron cargar los streams"
                    binding.statusText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando streams: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "❌ Error cargando streams:", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}