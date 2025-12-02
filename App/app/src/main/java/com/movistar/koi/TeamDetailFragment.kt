package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movistar.koi.adapters.PlayersAdapter
import com.movistar.koi.data.Player
import com.movistar.koi.data.Team
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.databinding.FragmentTeamDetailBinding

/**
 * Fragmento para mostrar el detalle completo de un equipo
 */
class TeamDetailFragment : Fragment() {

    private var _binding: FragmentTeamDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentTeam: Team
    private val playersList = mutableListOf<Player>()
    private lateinit var playersAdapter: PlayersAdapter

    companion object {
        private const val TAG = "TeamDetailFragment"

        /**
         * Crea una nueva instancia del fragmento de detalle de equipo
         */
        fun newInstance(team: Team): TeamDetailFragment {
            val fragment = TeamDetailFragment()
            val args = Bundle()
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

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener los argumentos
        val name = arguments?.getString("team_name") ?: "Equipo"
        val game = arguments?.getString("team_game") ?: ""
        val competition = arguments?.getString("team_competition") ?: ""
        val logo = arguments?.getString("team_logo") ?: ""
        val description = arguments?.getString("team_description") ?: ""
        val coach = arguments?.getString("team_coach") ?: ""
        val achievements = arguments?.getStringArrayList("team_achievements") ?: ArrayList()
        val playersIds = arguments?.getStringArrayList("team_players") ?: ArrayList()

        currentTeam = Team(
            name = name,
            game = game,
            competition = competition,
            logo = logo,
            description = description,
            coach = coach,
            achievements = achievements,
            players = playersIds
        )

        // Configurar la UI
        setupUI()

        // Configurar RecyclerView para jugadores
        setupPlayersRecyclerView()

        // Cargar jugadores reales
        loadPlayersFromFirebase(playersIds)
    }

    /**
     * Configura la interfaz de usuario con los datos del equipo
     */
    private fun setupUI() {
        // Cargar logo
        if (currentTeam.logo.isNotEmpty()) {
            Glide.with(requireContext())
                .load(currentTeam.logo)
                .placeholder(com.movistar.koi.R.color.koi_light_gray)
                .error(com.movistar.koi.R.color.koi_light_gray)
                .centerCrop()
                .into(binding.teamDetailLogo)
        }

        // Configurar textos
        binding.teamDetailGame.text = currentTeam.game
        binding.teamDetailName.text = currentTeam.name
        binding.teamDetailCompetition.text = currentTeam.competition
        binding.teamDetailCoach.text = "Entrenador: ${currentTeam.coach}"
        binding.teamDetailDescription.text = currentTeam.description

        // Configurar logros
        setupAchievements()

        Log.d(TAG, "Mostrando detalle de equipo: ${currentTeam.name}")
    }

    /**
     * Configura la sección de logros
     */
    private fun setupAchievements() {
        if (currentTeam.achievements.isNotEmpty()) {
            val achievementsText = currentTeam.achievements.joinToString("\n• ", "• ")
            binding.teamDetailAchievements.text = achievementsText
            binding.achievementsSection.visibility = View.VISIBLE
        } else {
            binding.achievementsSection.visibility = View.GONE
        }
    }

    /**
     * Configura el RecyclerView para jugadores
     */
    private fun setupPlayersRecyclerView() {
        playersAdapter = PlayersAdapter(playersList) { player ->
            Log.d(TAG, "Jugador clickeado: ${player.nickname}")
            // Navegar al detalle del jugador
            navigateToPlayerDetail(player)
        }

        binding.recyclerViewPlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playersAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * Navega al detalle del jugador
     */
    private fun navigateToPlayerDetail(player: Player) {
        Log.d(TAG, "Navegando al detalle de jugador: ${player.nickname}")

        val playerDetailFragment = PlayerDetailFragment.newInstance(player)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, playerDetailFragment)
            .addToBackStack("team_detail")
            .commit()
    }

    /**
     * Carga los jugadores reales desde Firebase
     */
    private fun loadPlayersFromFirebase(playerIds: List<String>) {
        if (playerIds.isEmpty()) {
            binding.teamDetailPlayersCount.text = "0 jugadores"
            binding.teamDetailPlayers.text = "No hay jugadores en este equipo"
            return
        }

        binding.teamDetailPlayersCount.text = "${playerIds.size} jugadores"
        binding.progressBarPlayers.visibility = View.VISIBLE

        Log.d(TAG, "Cargando ${playerIds.size} jugadores desde Firebase")

        // Limpiar lista actual
        playersList.clear()

        // Contador para saber cuándo hemos cargado todos los jugadores
        var loadedCount = 0
        val totalPlayers = playerIds.size

        playerIds.forEach { playerId ->
            FirebaseConfig.playersCollection
                .document(playerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            val player = document.toObject(Player::class.java)
                            player?.let {
                                playersList.add(it)
                                Log.d(TAG, "Jugador cargado: ${it.nickname}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error convirtiendo jugador $playerId: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "Jugador no encontrado: $playerId")
                    }

                    loadedCount++

                    if (loadedCount == totalPlayers) {
                        onPlayersLoaded()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error cargando jugador $playerId: ${exception.message}")
                    loadedCount++

                    if (loadedCount == totalPlayers) {
                        onPlayersLoaded()
                    }
                }
        }
    }

    /**
     * Se llama cuando todos los jugadores han sido cargados
     */
    private fun onPlayersLoaded() {
        binding.progressBarPlayers.visibility = View.GONE

        if (playersList.isNotEmpty()) {
            // Ordenar jugadores por rol
            val roleOrder = listOf("Top", "Jungla", "Mid Laner", "ADC", "Support", "Duelist", "Initiator", "Controller", "Sentinel")
            playersList.sortBy { player ->
                roleOrder.indexOf(player.role).takeIf { it >= 0 } ?: roleOrder.size
            }

            playersAdapter.updatePlayers(playersList)
            binding.teamDetailPlayers.text = "Plantilla completa cargada"

            Log.d(TAG, "${playersList.size} jugadores cargados y mostrados")
        } else {
            binding.teamDetailPlayers.text = "No se pudieron cargar los jugadores"
            Log.w(TAG, "No se pudieron cargar los jugadores")
        }
    }

    /**
     * Muestra un Toast temporal al hacer click en un jugador
     */
    private fun showPlayerToast(player: Player) {
        android.widget.Toast.makeText(
            requireContext(),
            "${player.nickname} - ${player.role}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Actualiza la vista
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}