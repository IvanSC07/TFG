package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.PlayersAdapter
import com.movistar.koi.data.Player
import com.movistar.koi.data.Team
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentManageTeamPlayersBinding
import com.movistar.koi.dialogs.PlayerDialog

class ManageTeamPlayersFragment : Fragment() {

    private var _binding: FragmentManageTeamPlayersBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentTeam: Team
    private val playersList = mutableListOf<Player>()
    private lateinit var playersAdapter: PlayersAdapter

    companion object {
        private const val TAG = "ManageTeamPlayersFragment"

        fun newInstance(team: Team): ManageTeamPlayersFragment {
            val fragment = ManageTeamPlayersFragment()
            val args = Bundle()
            args.putString("team_id", team.id)
            args.putString("team_name", team.name)
            args.putString("team_game", team.game)
            args.putString("team_competition", team.competition)
            args.putString("team_logo", team.logo)
            args.putString("team_description", team.description)
            args.putString("team_coach", team.coach)
            args.putStringArrayList("team_achievements", ArrayList(team.achievements))
            args.putStringArrayList("team_players", ArrayList(team.players))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTeamPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el equipo de los argumentos
        val teamId = arguments?.getString("team_id") ?: ""
        val teamName = arguments?.getString("team_name") ?: ""
        val teamGame = arguments?.getString("team_game") ?: ""
        val teamCompetition = arguments?.getString("team_competition") ?: ""
        val teamLogo = arguments?.getString("team_logo") ?: ""
        val teamDescription = arguments?.getString("team_description") ?: ""
        val teamCoach = arguments?.getString("team_coach") ?: ""
        val teamAchievements = arguments?.getStringArrayList("team_achievements") ?: ArrayList()
        val teamPlayers = arguments?.getStringArrayList("team_players") ?: ArrayList()

        currentTeam = Team(
            id = teamId,
            name = teamName,
            game = teamGame,
            competition = teamCompetition,
            logo = teamLogo,
            description = teamDescription,
            coach = teamCoach,
            achievements = teamAchievements,
            players = teamPlayers
        )

        setupUI()
        loadTeamPlayersData() // CAMBIÉ EL NOMBRE DEL MÉTODO
    }

    private fun setupUI() {
        binding.toolbar.title = "Jugadores de ${currentTeam.name}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.fabAddPlayer.setOnClickListener {
            showAddPlayerDialog()
        }

        playersAdapter = PlayersAdapter(playersList) { player ->
            showPlayerActionsDialog(player)
        }

        binding.recyclerViewPlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playersAdapter
            setHasFixedSize(true)
        }
    }

    // CAMBIÉ EL NOMBRE A loadTeamPlayersData PARA EVITAR CONFLICTO
    private fun loadTeamPlayersData() {
        binding.progressBar.visibility = View.VISIBLE

        // Si el equipo no tiene jugadores, mostrar estado vacío
        if (currentTeam.players.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            binding.statusText.text = "No hay jugadores en este equipo"
            binding.statusText.visibility = View.VISIBLE
            return
        }

        // Limpiar lista actual
        playersList.clear()

        // Cargar cada jugador por su ID
        currentTeam.players.forEach { playerId ->
            FirebaseConfig.playersCollection
                .document(playerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val player = document.toObject(Player::class.java)
                        player?.let {
                            playersList.add(it)
                            playersAdapter.updatePlayers(playersList)
                        }
                    }

                    // Ocultar progressBar cuando se hayan cargado todos
                    if (playersList.size == currentTeam.players.size) {
                        binding.progressBar.visibility = View.GONE
                        if (playersList.isEmpty()) {
                            binding.statusText.text = "No hay jugadores en este equipo"
                            binding.statusText.visibility = View.VISIBLE
                        } else {
                            binding.statusText.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error cargando jugador: ${exception.message}")
                }
        }
    }

    private fun showAddPlayerDialog() {
        val dialog = PlayerDialog.newInstance()
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, "add_player_dialog")
    }

    private fun showPlayerActionsDialog(player: Player) {
        val options = arrayOf("Editar", "Ver Detalles", "Quitar del equipo", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${player.nickname}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editPlayer(player)
                    1 -> showPlayerDetails(player)
                    2 -> removePlayerFromTeam(player)
                }
            }
            .show()
    }

    private fun editPlayer(player: Player) {
        val dialog = PlayerDialog.newInstance(player)
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, "edit_player_dialog")
    }

    private fun showPlayerDetails(player: Player) {
        val detailFragment = PlayerDetailFragment.newInstance(player)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("manage_team_players")
            .commit()
    }

    private fun removePlayerFromTeam(player: Player) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Quitar Jugador")
            .setMessage("¿Estás seguro de que quieres quitar a ${player.nickname} del equipo ${currentTeam.name}?")
            .setPositiveButton("Quitar") { _, _ ->
                performRemovePlayerFromTeam(player)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performRemovePlayerFromTeam(player: Player) {
        // Actualizar la lista de jugadores del equipo (quitar el ID del jugador)
        val updatedPlayers = currentTeam.players.toMutableList().apply {
            remove(player.id)
        }

        // Actualizar el equipo en Firestore
        FirebaseConfig.teamsCollection
            .document(currentTeam.id)
            .update("players", updatedPlayers)
            .addOnSuccessListener {
                // También actualizar el campo "team" del jugador a vacío
                FirebaseConfig.playersCollection
                    .document(player.id)
                    .update("team", "")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Jugador quitado del equipo", Toast.LENGTH_SHORT).show()
                        loadTeamPlayersData() // Recargar la lista
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error actualizando jugador", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error actualizando jugador: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error quitando jugador del equipo", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error quitando jugador del equipo: ${e.message}")
            }
    }

    fun refreshTeamPlayers() {
        loadTeamPlayersData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}