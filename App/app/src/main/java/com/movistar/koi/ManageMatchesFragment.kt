package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.movistar.koi.adapters.MatchesAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Match
import com.movistar.koi.databinding.FragmentManageMatchesBinding
import java.util.Date

class ManageMatchesFragment : Fragment() {

    private var _binding: FragmentManageMatchesBinding? = null
    private val binding get() = _binding!!
    private val matchesList = mutableListOf<Match>()
    private lateinit var matchesAdapter: MatchesAdapter

    companion object {
        private const val TAG = "ManageMatchesFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadMatches()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.title = "Gestionar Partidos"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configurar botón de agregar
        binding.fabAddMatch.setOnClickListener {
            showAddMatchDialog()
        }

        // Configurar RecyclerView
        matchesAdapter = MatchesAdapter(matchesList) { match ->
            showMatchActionsDialog(match)
        }

        binding.recyclerViewMatches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchesAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadMatches() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.matchesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                matchesList.clear()

                for (document in documents) {
                    try {
                        val match = document.toObject(Match::class.java)
                        matchesList.add(match)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo partido: ${e.message}")
                    }
                }

                matchesAdapter.updateMatches(matchesList)

                if (matchesList.isEmpty()) {
                    binding.statusText.text = "No hay partidos programados"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.statusText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando partidos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando partidos:", exception)
            }
    }

    private fun showAddMatchDialog() {
        // TODO: Implementar diálogo para agregar partido
        android.widget.Toast.makeText(requireContext(), "Agregar partido - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showMatchActionsDialog(match: Match) {
        val options = arrayOf("Editar", "Eliminar", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${match.opponent}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editMatch(match)
                    1 -> deleteMatch(match)
                }
            }
            .show()
    }

    private fun editMatch(match: Match) {
        // TODO: Implementar edición de partido
        android.widget.Toast.makeText(requireContext(), "Editar partido - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun deleteMatch(match: Match) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Partido")
            .setMessage("¿Estás seguro de que quieres eliminar el partido contra ${match.opponent}?")
            .setPositiveButton("Eliminar") { _, _ ->
                performDeleteMatch(match)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDeleteMatch(match: Match) {
        // Buscar el documento por ID o crear uno temporal para eliminación
        FirebaseConfig.matchesCollection
            .whereEqualTo("opponent", match.opponent)
            .whereEqualTo("date", match.date)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(requireContext(), "Partido eliminado", android.widget.Toast.LENGTH_SHORT).show()
                            loadMatches() // Recargar lista
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(requireContext(), "Error eliminando partido", android.widget.Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Error eliminando partido: ${e.message}")
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}