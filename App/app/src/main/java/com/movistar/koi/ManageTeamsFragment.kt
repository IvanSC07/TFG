package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.TeamsAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Team
import com.movistar.koi.databinding.FragmentManageTeamsBinding

class ManageTeamsFragment : Fragment() {

    private var _binding: FragmentManageTeamsBinding? = null
    private val binding get() = _binding!!
    private val teamsList = mutableListOf<Team>()
    private lateinit var teamsAdapter: TeamsAdapter

    companion object {
        private const val TAG = "ManageTeamsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadTeams()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.title = "Gestionar Equipos"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configurar botón de agregar
        binding.fabAddTeam.setOnClickListener {
            showAddTeamDialog()
        }

        // Configurar RecyclerView
        teamsAdapter = TeamsAdapter(teamsList) { team ->
            showTeamActionsDialog(team)
        }

        binding.recyclerViewTeams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = teamsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadTeams() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.teamsCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                teamsList.clear()

                for (document in documents) {
                    try {
                        val team = document.toObject(Team::class.java)
                        teamsList.add(team)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo equipo: ${e.message}")
                    }
                }

                teamsAdapter.updateTeams(teamsList)

                if (teamsList.isEmpty()) {
                    binding.statusText.text = "No hay equipos registrados"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.statusText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando equipos: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando equipos:", exception)
            }
    }

    private fun showAddTeamDialog() {
        android.widget.Toast.makeText(requireContext(), "Agregar equipo - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showTeamActionsDialog(team: Team) {
        val options = arrayOf("Editar", "Gestionar Jugadores", "Eliminar", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${team.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editTeam(team)
                    1 -> manageTeamPlayers(team)
                    2 -> deleteTeam(team)
                }
            }
            .show()
    }

    private fun editTeam(team: Team) {
        android.widget.Toast.makeText(requireContext(), "Editar equipo - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun manageTeamPlayers(team: Team) {
        android.widget.Toast.makeText(requireContext(), "Gestionar jugadores - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun deleteTeam(team: Team) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Equipo")
            .setMessage("¿Estás seguro de que quieres eliminar el equipo ${team.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                performDeleteTeam(team)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDeleteTeam(team: Team) {
        FirebaseConfig.teamsCollection
            .whereEqualTo("name", team.name)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(requireContext(), "Equipo eliminado", android.widget.Toast.LENGTH_SHORT).show()
                            loadTeams()
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(requireContext(), "Error eliminando equipo", android.widget.Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Error eliminando equipo: ${e.message}")
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}