package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.movistar.koi.data.Player
import com.movistar.koi.databinding.FragmentPlayerDetailBinding

/**
 * Fragmento para mostrar el detalle completo de un jugador
 */
class PlayerDetailFragment : Fragment() {

    private var _binding: FragmentPlayerDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentPlayer: Player

    companion object {
        private const val TAG = "PlayerDetailFragment"

        /**
         * Crea una nueva instancia del fragmento de detalle de jugador
         */
        fun newInstance(player: Player): PlayerDetailFragment {
            val fragment = PlayerDetailFragment()
            val args = Bundle()
            args.putString("player_id", player.id)
            args.putString("player_name", player.name)
            args.putString("player_nickname", player.nickname)
            args.putString("player_role", player.role)
            args.putString("player_team", player.team)
            args.putString("player_photo", player.photo)
            args.putString("player_nationality", player.nationality)
            args.putInt("player_age", player.age)
            args.putString("player_bio", player.bio)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener los argumentos
        val id = arguments?.getString("player_id") ?: ""
        val name = arguments?.getString("player_name") ?: "Jugador"
        val nickname = arguments?.getString("player_nickname") ?: ""
        val role = arguments?.getString("player_role") ?: ""
        val team = arguments?.getString("player_team") ?: ""
        val photo = arguments?.getString("player_photo") ?: ""
        val nationality = arguments?.getString("player_nationality") ?: ""
        val age = arguments?.getInt("player_age") ?: 0
        val bio = arguments?.getString("player_bio") ?: ""

        // Reconstruir el objeto Player
        currentPlayer = Player(
            id = id,
            name = name,
            nickname = nickname,
            role = role,
            team = team,
            photo = photo,
            nationality = nationality,
            age = age,
            bio = bio
        )

        // Configurar la UI
        setupUI()

        Log.d(TAG, "🎮 Mostrando detalle de jugador: ${currentPlayer.nickname}")
    }

    /**
     * Configura la interfaz de usuario con los datos del jugador
     */
    private fun setupUI() {
        // Cargar foto del jugador
        if (currentPlayer.photo.isNotEmpty()) {
            Glide.with(requireContext())
                .load(currentPlayer.photo)
                .placeholder(com.movistar.koi.R.color.koi_light_gray)
                .error(com.movistar.koi.R.color.koi_light_gray)
                .centerCrop()
                .into(binding.playerDetailPhoto)
        }

        // Configurar información básica
        binding.playerDetailRole.text = currentPlayer.role
        binding.playerDetailNickname.text = currentPlayer.nickname
        binding.playerDetailName.text = currentPlayer.name

        // Configurar información adicional
        binding.playerDetailNationality.text = "${getFlagEmoji(currentPlayer.nationality)} ${currentPlayer.nationality}"
        binding.playerDetailAge.text = "${currentPlayer.age} años"
        binding.playerDetailTeam.text = currentPlayer.team

        // Configurar biografía
        if (currentPlayer.bio.isNotEmpty()) {
            binding.playerDetailBio.text = currentPlayer.bio
        } else {
            binding.playerDetailBio.text = "No hay biografía disponible para este jugador."
        }
    }

    /**
     * Obtiene emoji de bandera basado en la nacionalidad
     */
    private fun getFlagEmoji(nationality: String): String {
        return when (nationality.lowercase()) {
            "españa", "spain" -> "🇪🇸"
            "francia", "france" -> "🇫🇷"
            "italia", "italy" -> "🇮🇹"
            "alemania", "germany" -> "🇩🇪"
            "reino unido", "uk" -> "🇬🇧"
            "estados unidos", "usa" -> "🇺🇸"
            else -> "🏴"
        }
    }
    //////////////////////////////AGREGAR TODAS LAS BANDERAS DE EUROPA///////////////////////////

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}