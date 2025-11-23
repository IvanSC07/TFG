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
import com.movistar.koi.adapters.MatchesAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Match
import com.movistar.koi.databinding.FragmentManageMatchesBinding
import com.movistar.koi.dialogs.MatchDialog

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
        binding.toolbar.title = "Gestionar Partidos"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.fabAddMatch.setOnClickListener {
            showAddMatchDialog()
        }

        // Inicializar el adaptador con una lista vacía primero
        matchesAdapter = MatchesAdapter(mutableListOf()) { match ->
            showMatchActionsDialog(match)
        }

        binding.recyclerViewMatches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchesAdapter
            setHasFixedSize(true)
        }
    }

    fun loadMatches() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewMatches.visibility = View.GONE
        binding.statusText.visibility = View.GONE

        FirebaseConfig.matchesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                // Crear una nueva lista en lugar de limpiar la existente
                val newMatchesList = mutableListOf<Match>()

                for (document in documents) {
                    try {
                        val match = document.toObject(Match::class.java).copy(id = document.id)
                        newMatchesList.add(match)
                        Log.d(TAG, "Partido cargado: ${match.opponent} - ID: ${match.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo partido: ${e.message}")
                    }
                }

                // Actualizar la lista y notificar al adaptador
                matchesList.clear()
                matchesList.addAll(newMatchesList)
                matchesAdapter.updateMatches(newMatchesList)

                if (matchesList.isEmpty()) {
                    binding.statusText.text = "No hay partidos programados"
                    binding.statusText.visibility = View.VISIBLE
                    binding.recyclerViewMatches.visibility = View.GONE
                } else {
                    binding.recyclerViewMatches.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando partidos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                binding.recyclerViewMatches.visibility = View.GONE
                Log.e(TAG, "Error cargando partidos:", exception)
            }
    }

    private fun showAddMatchDialog() {
        val dialog = MatchDialog.newInstance()
        dialog.setTargetFragment(this, 0) // ESTABLECER TARGET FRAGMENT
        dialog.show(parentFragmentManager, "match_dialog")
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
        val dialog = MatchDialog.newInstance(match)
        dialog.setTargetFragment(this, 0) // ESTABLECER TARGET FRAGMENT
        dialog.show(parentFragmentManager, "edit_match_dialog")
    }

    private fun deleteMatch(match: Match) {
        if (match.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No se puede eliminar - ID inválido", Toast.LENGTH_LONG).show()
            return
        }

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
        if (match.id.isEmpty()) {
            Toast.makeText(requireContext(), "Error: ID de partido inválido", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Intentando eliminar partido con ID: ${match.id}")

        FirebaseConfig.matchesCollection
            .document(match.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Partido eliminado", Toast.LENGTH_SHORT).show()
                loadMatches()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error eliminando partido: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error eliminando partido: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}