package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.TeamsAdapter
import com.movistar.koi.data.Team
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentTeamsBinding

/**
 * Fragmento para mostrar los equipos de Movistar KOI
 */
class TeamsFragment : Fragment() {

    private var _binding: FragmentTeamsBinding? = null
    private val binding get() = _binding!!
    private val teamsList = mutableListOf<Team>()
    private lateinit var teamsAdapter: TeamsAdapter

    companion object {
        private const val TAG = "TeamsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTeams()
    }

    private fun setupRecyclerView() {
        teamsAdapter = TeamsAdapter(teamsList) { team ->
            Log.d(TAG, "Equipo clickeado: ${team.name}")
            // Navegar al detalle del equipo
            navigateToTeamDetail(team)
        }

        binding.recyclerViewTeams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = teamsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadTeams() {
        Log.d(TAG, "Cargando equipos desde Firebase")

        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Cargando equipos..."
        binding.recyclerViewTeams.visibility = View.GONE

        FirebaseConfig.teamsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                teamsList.clear()

                Log.d(TAG, "✅ Equipos encontrados: ${documents.size()}")

                if (documents.isEmpty) {
                    binding.statusText.text = "No hay equipos disponibles"
                    binding.statusText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val team = document.toObject(Team::class.java)
                        teamsList.add(team)
                        Log.d(TAG, "🏆 Equipo: ${team.name} - ${team.game}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error convirtiendo equipo: ${e.message}")
                    }
                }

                if (teamsList.isNotEmpty()) {
                    // Ordenar por juego
                    teamsList.sortBy { it.game }

                    teamsAdapter.updateTeams(teamsList)

                    binding.recyclerViewTeams.visibility = View.VISIBLE
                    binding.statusText.visibility = View.GONE

                    Log.d(TAG, "✅ ${teamsList.size} equipos mostrados")
                } else {
                    binding.statusText.text = "No se pudieron cargar los equipos"
                    binding.statusText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando equipos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "❌ Error cargando equipos:", exception)
            }
    }

    /**
     * Navega al detalle del equipo
     */
    private fun navigateToTeamDetail(team: Team) {
        Log.d(TAG, "Navegando al detalle de: ${team.name}")

        val teamDetailFragment = TeamDetailFragment.newInstance(team)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, teamDetailFragment)
            .addToBackStack("teams_list")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}