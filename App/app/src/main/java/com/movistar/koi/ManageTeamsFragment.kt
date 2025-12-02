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
import com.movistar.koi.dialogs.TeamDialog

/**
 * Fragmento para gestionar equipos
 */
class ManageTeamsFragment : Fragment() {

    private var _binding: FragmentManageTeamsBinding? = null
    private val binding get() = _binding!!
    private val teamsList = mutableListOf<Team>()
    private lateinit var teamsAdapter: TeamsAdapter

    companion object {
        private const val TAG = "ManageTeamsFragment"
    }

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadTeams()
    }

    /**
     * Configura la interfaz de usuario
     */
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

    /**
     * Carga los equipos
     */
    fun loadTeams() {
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

    /**
     * Muestra el diálogo para agregar un equipo
     */
    private fun showAddTeamDialog() {
        val dialog = TeamDialog.newInstance()
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, "team_dialog")
    }

    /**
     * Muestra el diálogo de acciones para un equipo
     */
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

    /**
     * Edita un equipo
     */
    private fun editTeam(team: Team) {
        val dialog = TeamDialog.newInstance(team)
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, "edit_team_dialog")
    }

    /**
     * Gestiona los jugadores de un equipo
     */
    private fun manageTeamPlayers(team: Team) {
        val manageTeamPlayersFragment = ManageTeamPlayersFragment.newInstance(team)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, manageTeamPlayersFragment)
            .addToBackStack("manage_teams")
            .commit()
    }

    /**
     * Elimina un equipo
     */
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

    /**
     * Realiza la eliminación de un equipo
     */
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

    /**
     * Actualiza la lista de equipos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}