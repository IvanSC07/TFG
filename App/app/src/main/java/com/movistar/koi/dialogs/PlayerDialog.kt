package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.movistar.koi.ManagePlayersFragment
import com.movistar.koi.ManageTeamPlayersFragment
import com.movistar.koi.R
import com.movistar.koi.data.Player
import com.movistar.koi.data.Team
import com.movistar.koi.databinding.DialogPlayerBinding
import java.util.*

/**
 * Dialogo para crear o editar un jugador
 */
class PlayerDialog : DialogFragment() {

    private var _binding: DialogPlayerBinding? = null
    private val binding get() = _binding!!
    private var existingPlayer: Player? = null
    private var imageUri: Uri? = null
    private val teamsList = mutableListOf<Team>()
    private lateinit var teamsAdapter: ArrayAdapter<String>
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Abre la galería para seleccionar una imagen
     */
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.playerPhoto.setImageURI(it)
            binding.textImageStatus.text = "Imagen seleccionada - Subir al guardar"
        }
    }

    /**
     * Instancia del diálogo
     */
    companion object {
        private const val TAG = "PlayerDialog"

        /**
         * Crea una nueva instancia del diálogo
         * @param player Jugador existente para editar, si es nulo se crea uno nuevo
         */
        fun newInstance(player: Player? = null): PlayerDialog {
            val dialog = PlayerDialog()
            player?.let {
                val args = Bundle()
                args.putString("id", it.id)
                args.putString("name", it.name)
                args.putString("nickname", it.nickname)
                args.putString("role", it.role)
                args.putString("team", it.team)
                args.putString("photo", it.photo)
                args.putString("nationality", it.nationality)
                args.putInt("age", it.age)
                args.putString("bio", it.bio)
                dialog.arguments = args
            }
            return dialog
        }
    }

    /**
     * Crea el diálogo
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPlayerBinding.inflate(LayoutInflater.from(requireContext()))

        // Configurar el Spinner de equipos
        setupTeamsSpinner()

        // Cargar equipos desde Firebase
        loadTeamsFromFirestore()

        // Cargar datos existentes si estamos editando
        loadExistingData()

        // Configurar listeners
        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingPlayer == null) "Crear Jugador" else "Editar Jugador")
            .setPositiveButton("Guardar") { _, _ ->
                binding.root.post { savePlayer() }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Configura el Spinner de equipos
     */
    private fun setupTeamsSpinner() {
        teamsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        teamsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTeam.adapter = teamsAdapter
    }

    /**
     * Carga equipos desde Firebase
     */
    private fun loadTeamsFromFirestore() {
        FirebaseFirestore.getInstance().collection("teams")
            .get()
            .addOnSuccessListener { documents ->
                teamsList.clear()
                val teamNames = mutableListOf<String>()

                for (document in documents) {
                    try {
                        val team = document.toObject(Team::class.java)
                        teamsList.add(team)
                        teamNames.add(team.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo equipo: ${e.message}")
                    }
                }

                teamsAdapter.clear()
                teamNames.forEach { teamName ->
                    teamsAdapter.add(teamName)
                }
                teamsAdapter.notifyDataSetChanged()

                // Si estamos editando, seleccionar el equipo del jugador
                existingPlayer?.team?.let { teamName ->
                    val position = teamsAdapter.getPosition(teamName)
                    if (position >= 0) {
                        binding.spinnerTeam.setSelection(position)
                    }
                }

                Log.d(TAG, "Cargados ${teamNames.size} equipos")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error cargando equipos:", exception)
                val defaultTeams = arrayOf("Movistar KOI LoL", "Movistar KOI VALORANT", "Movistar KOI TFT")
                teamsAdapter.clear()
                defaultTeams.forEach { team ->
                    teamsAdapter.add(team)
                }
                teamsAdapter.notifyDataSetChanged()
            }
    }

    /**
     * Carga datos existentes del jugador
     */
    private fun loadExistingData() {
        arguments?.let { args ->
            val playerId = args.getString("id") ?: ""

            existingPlayer = Player(
                id = playerId,
                name = args.getString("name") ?: "",
                nickname = args.getString("nickname") ?: "",
                role = args.getString("role") ?: "",
                team = args.getString("team") ?: "",
                photo = args.getString("photo") ?: "",
                nationality = args.getString("nationality") ?: "",
                age = args.getInt("age", 0),
                bio = args.getString("bio") ?: ""
            )

            // Llenar los campos
            binding.editTextName.setText(existingPlayer?.name)
            binding.editTextNickname.setText(existingPlayer?.nickname)
            binding.editTextRole.setText(existingPlayer?.role)
            binding.editTextNationality.setText(existingPlayer?.nationality)
            binding.editTextAge.setText(existingPlayer?.age.toString())
            binding.editTextBio.setText(existingPlayer?.bio)


            // Cargar foto existente
            if (!existingPlayer?.photo.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(existingPlayer?.photo)
                    .placeholder(R.color.koi_light_gray)
                    .into(binding.playerPhoto)
                binding.textImageStatus.text = "Imagen cargada"
            }
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupListeners() {
        // Seleccionar imagen de galería
        binding.buttonSelectPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Seleccionar imagen por URL
        binding.buttonSelectPhotoUrl.setOnClickListener {
            showImageUrlDialog()
        }

        binding.buttonRemovePhoto.setOnClickListener {
            imageUri = null
            binding.playerPhoto.setImageResource(R.color.koi_light_gray)
            binding.textImageStatus.text = "Sin imagen"
            existingPlayer = existingPlayer?.copy(photo = "") ?: Player(photo = "")
        }
    }

    /**
     * Muestra el diálogo para seleccionar la URL de la imagen
     */
    private fun showImageUrlDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_url, null)
        val editTextUrl = dialogView.findViewById<android.widget.EditText>(R.id.editTextImageUrl)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("URL de la Foto")
            .setPositiveButton("Cargar") { _, _ ->
                val url = editTextUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadImageFromUrl(url)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Carga una imagen desde una URL
     */
    private fun loadImageFromUrl(url: String) {
        binding.textImageStatus.text = "Cargando imagen..."

        Glide.with(requireContext())
            .load(url)
            .placeholder(R.color.koi_light_gray)
            .error(R.color.koi_light_gray)
            .into(binding.playerPhoto)

        binding.playerPhoto.postDelayed({
            binding.textImageStatus.text = "Imagen cargada"
            existingPlayer = existingPlayer?.copy(photo = url) ?: Player(photo = url)
        }, 1000)
    }

    /**
     * Guarda el jugador
     */
    private fun savePlayer() {
        val name = binding.editTextName.text.toString().trim()
        val nickname = binding.editTextNickname.text.toString().trim()
        val role = binding.editTextRole.text.toString().trim()
        val nationality = binding.editTextNationality.text.toString().trim()
        val ageText = binding.editTextAge.text.toString().trim()
        val bio = binding.editTextBio.text.toString().trim()
        val team = binding.spinnerTeam.selectedItem.toString()

        if (name.isEmpty() || nickname.isEmpty() || role.isEmpty() || nationality.isEmpty() || ageText.isEmpty()) {
            showToast("Completa todos los campos obligatorios")
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 0) {
            showToast("Edad inválida")
            return
        }

        // Determinar si estamos creando o editando
        val isEditing = !existingPlayer?.id.isNullOrEmpty()

        val playerToSave = if (isEditing) {
            existingPlayer!!.copy(
                name = name,
                nickname = nickname,
                role = role,
                team = team,
                nationality = nationality,
                age = age,
                bio = bio
            )
        } else {
            Player(
                name = name,
                nickname = nickname,
                role = role,
                team = team,
                nationality = nationality,
                age = age,
                bio = bio
            )
        }

        if (isEditing) {
            updatePlayer(playerToSave)
        } else {
            createPlayer(playerToSave)
        }
    }

    /**
     * Crea un nuevo jugador
     */
    private fun createPlayer(player: Player) {
        val newDocRef = db.collection("players").document()
        val playerWithId = player.copy(id = newDocRef.id)

        newDocRef.set(playerWithId)
            .addOnSuccessListener {
                showToast("Jugador creado exitosamente")
                // Actualizar el equipo con este nuevo jugador
                updateTeamWithPlayer(playerWithId)
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error creando jugador: ${e.message}")
            }
    }

    /**
     * Actualiza un jugador
     */
    private fun updatePlayer(player: Player) {
        if (player.id.isEmpty()) {
            showToast("Error: ID de jugador inválido")
            return
        }

        db.collection("players").document(player.id)
            .set(player)
            .addOnSuccessListener {
                showToast("Jugador actualizado exitosamente")
                // Actualizar el equipo si cambió de equipo
                updateTeamWithPlayer(player)
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error actualizando jugador: ${e.message}")
            }
    }

    /**
     * Actualiza el equipo con el nuevo jugador
     */
    private fun updateTeamWithPlayer(player: Player) {
        db.collection("teams")
            .whereEqualTo("name", player.team)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val team = document.toObject(Team::class.java)
                        // Actualizar la lista de jugadores del equipo
                        val updatedPlayers = team.players.toMutableList().apply {
                            if (!contains(player.id)) {
                                add(player.id)
                            }
                        }

                        // Actualizar el equipo en Firestore
                        document.reference.update("players", updatedPlayers)
                            .addOnSuccessListener {
                                Log.d(TAG, "Equipo actualizado con el nuevo jugador")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error actualizando equipo: ${e.message}")
                            }
                    }
                } else {
                    Log.w(TAG, "No se encontró el equipo: ${player.team}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error buscando equipo: ${e.message}")
            }

        // Actualiza el equipo anterior si estamos editando y cambió de equipo
        existingPlayer?.let { oldPlayer ->
            if (oldPlayer.team != player.team && oldPlayer.team.isNotEmpty()) {
                // Quita el jugador del equipo anterior
                db.collection("teams")
                    .whereEqualTo("name", oldPlayer.team)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            for (document in documents) {
                                val oldTeam = document.toObject(Team::class.java)
                                val updatedPlayers = oldTeam.players.toMutableList().apply {
                                    remove(player.id)
                                }

                                document.reference.update("players", updatedPlayers)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Jugador quitado del equipo anterior")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error quitando jugador del equipo anterior: ${e.message}")
                                    }
                            }
                        }
                    }
            }
        }
    }

    /**
     * Muestra un Toast
     */
    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cierra el diálogo
     */
    private fun dismissSafely() {
        if (isAdded) {
            dismiss()
        }
    }

    /**
     * Notifica al padre que se debe recargar la lista de jugadores
     */
    private fun notifyParentToReload() {
        (targetFragment as? ManagePlayersFragment)?.loadPlayers()
        (targetFragment as? ManageTeamPlayersFragment)?.refreshTeamPlayers()
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}