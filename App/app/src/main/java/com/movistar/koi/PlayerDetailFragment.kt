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
 * Fragmento para mostrar el detalle de un jugador
 */
class PlayerDetailFragment : Fragment() {

    private var _binding: FragmentPlayerDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentPlayer: Player

    /**
     * Crea la vista
     */
    companion object {
        private const val TAG = "PlayerDetailFragment"

        /**
         * Crea una nueva instancia del fragmento con los argumentos necesarios
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

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
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

        // Configura información básica
        binding.playerDetailRole.text = currentPlayer.role
        binding.playerDetailNickname.text = currentPlayer.nickname
        binding.playerDetailName.text = currentPlayer.name

        // Configura información adicional
        binding.playerDetailNationality.text = "${getFlagEmoji(currentPlayer.nationality)} ${currentPlayer.nationality}"
        binding.playerDetailAge.text = "${currentPlayer.age} años"
        binding.playerDetailTeam.text = currentPlayer.team

        // Configura biografía
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
            "reino unido", "uk", "inglaterra" -> "🇬🇧"
            "estados unidos", "usa" -> "🇺🇸"
            "canadá", "canada" -> "🇨🇦"
            "brasil", "brazil" -> "🇧🇷"
            "argentina" -> "🇦🇷"
            "méxico", "mexico" -> "🇲🇽"
            "colombia" -> "🇨🇴"
            "chile" -> "🇨🇱"
            "perú", "peru" -> "🇵🇪"
            "venezuela" -> "🇻🇪"
            "ecuador" -> "🇪🇨"
            "uruguay" -> "🇺🇾"
            "paraguay" -> "🇵🇾"
            "bolivia" -> "🇧🇴"
            "portugal" -> "🇵🇹"
            "bélgica", "belgium" -> "🇧🇪"
            "países bajos", "netherlands" -> "🇳🇱"
            "suiza", "switzerland" -> "🇨🇭"
            "suecia", "sweden" -> "🇸🇪"
            "noruega", "norway" -> "🇳🇴"
            "dinamarca", "denmark" -> "🇩🇰"
            "finlandia", "finland" -> "🇫🇮"
            "polonia", "poland" -> "🇵🇱"
            "rusia", "russia" -> "🇷🇺"
            "ucrania", "ukraine" -> "🇺🇦"
            "turquía", "turkey" -> "🇹🇷"
            "grecia", "greece" -> "🇬🇷"
            "china" -> "🇨🇳"
            "japón", "japan" -> "🇯🇵"
            "corea del sur", "south korea" -> "🇰🇷"
            "australia" -> "🇦🇺"
            "nueva zelanda", "new zealand" -> "🇳🇿"
            else -> "🏴"
        }
    }

    /**
     * Actualiza la vista
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}