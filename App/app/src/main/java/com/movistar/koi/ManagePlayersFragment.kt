package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.PlayersAdapter
import com.movistar.koi.data.FirebaseConfig
import com.movistar.koi.data.Player
import com.movistar.koi.databinding.FragmentManagePlayersBinding

class ManagePlayersFragment : Fragment() {

    private var _binding: FragmentManagePlayersBinding? = null
    private val binding get() = _binding!!
    private val playersList = mutableListOf<Player>()
    private lateinit var playersAdapter: PlayersAdapter

    companion object {
        private const val TAG = "ManagePlayersFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagePlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadPlayers()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.title = "Gestionar Jugadores"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configurar botón de agregar
        binding.fabAddPlayer.setOnClickListener {
            showAddPlayerDialog()
        }

        // Configurar RecyclerView
        playersAdapter = PlayersAdapter(playersList) { player ->
            showPlayerActionsDialog(player)
        }

        binding.recyclerViewPlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playersAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadPlayers() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseConfig.playersCollection
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                playersList.clear()

                for (document in documents) {
                    try {
                        val player = document.toObject(Player::class.java)
                        playersList.add(player)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo jugador: ${e.message}")
                    }
                }

                // Ordenar por nickname
                playersList.sortBy { it.nickname }

                playersAdapter.updatePlayers(playersList)

                if (playersList.isEmpty()) {
                    binding.statusText.text = "No hay jugadores registrados"
                    binding.statusText.visibility = View.VISIBLE
                } else {
                    binding.statusText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Error cargando jugadores: ${exception.message}"
                binding.statusText.visibility = View.VISIBLE
                Log.e(TAG, "Error cargando jugadores:", exception)
            }
    }

    private fun showAddPlayerDialog() {
        android.widget.Toast.makeText(requireContext(), "Agregar jugador - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showPlayerActionsDialog(player: Player) {
        val options = arrayOf("Editar", "Ver Detalles", "Eliminar", "Cancelar")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Acciones para: ${player.nickname}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editPlayer(player)
                    1 -> showPlayerDetails(player)
                    2 -> deletePlayer(player)
                }
            }
            .show()
    }

    private fun editPlayer(player: Player) {
        android.widget.Toast.makeText(requireContext(), "Editar jugador - En desarrollo", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showPlayerDetails(player: Player) {
        val detailFragment = PlayerDetailFragment.newInstance(player)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("manage_players")
            .commit()
    }

    private fun deletePlayer(player: Player) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Jugador")
            .setMessage("¿Estás seguro de que quieres eliminar a ${player.nickname}?")
            .setPositiveButton("Eliminar") { _, _ ->
                performDeletePlayer(player)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDeletePlayer(player: Player) {
        FirebaseConfig.playersCollection
            .whereEqualTo("nickname", player.nickname)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(requireContext(), "Jugador eliminado", android.widget.Toast.LENGTH_SHORT).show()
                            loadPlayers()
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(requireContext(), "Error eliminando jugador", android.widget.Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Error eliminando jugador: ${e.message}")
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}