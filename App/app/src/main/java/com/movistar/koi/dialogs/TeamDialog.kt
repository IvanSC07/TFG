package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.movistar.koi.ManageTeamsFragment
import com.movistar.koi.R
import com.movistar.koi.data.Team
import com.movistar.koi.databinding.DialogTeamBinding
import java.util.*

class TeamDialog : DialogFragment() {

    private var _binding: DialogTeamBinding? = null
    private val binding get() = _binding!!
    private var existingTeam: Team? = null
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Para seleccionar imagen de la galería
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.teamLogo.setImageURI(it)
            binding.textImageStatus.text = "Imagen seleccionada - Subir al guardar"
        }
    }

    companion object {
        private const val TAG = "TeamDialog"

        fun newInstance(team: Team? = null): TeamDialog {
            val dialog = TeamDialog()
            team?.let {
                val args = Bundle()
                args.putString("id", it.id)
                args.putString("name", it.name)
                args.putString("game", it.game)
                args.putString("competition", it.competition)
                args.putString("logo", it.logo)
                args.putString("description", it.description)
                args.putString("coach", it.coach)
                args.putStringArrayList("achievements", ArrayList(it.achievements))
                dialog.arguments = args
            }
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTeamBinding.inflate(LayoutInflater.from(requireContext()))

        // Cargar datos existentes si estamos editando
        loadExistingData()

        // Configurar listeners
        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingTeam == null) "Crear Equipo" else "Editar Equipo")
            .setPositiveButton("Guardar") { _, _ ->
                binding.root.post { saveTeam() }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun loadExistingData() {
        arguments?.let { args ->
            val teamId = args.getString("id") ?: ""

            existingTeam = Team(
                id = teamId,
                name = args.getString("name") ?: "",
                game = args.getString("game") ?: "",
                competition = args.getString("competition") ?: "",
                logo = args.getString("logo") ?: "",
                description = args.getString("description") ?: "",
                coach = args.getString("coach") ?: "",
                achievements = args.getStringArrayList("achievements") ?: emptyList()
            )

            // Llenar los campos
            binding.editTextName.setText(existingTeam?.name)
            binding.editTextGame.setText(existingTeam?.game) // Ahora es EditText
            binding.editTextCompetition.setText(existingTeam?.competition)
            binding.editTextDescription.setText(existingTeam?.description)
            binding.editTextCoach.setText(existingTeam?.coach)

            // Cargar logo existente
            if (!existingTeam?.logo.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(existingTeam?.logo)
                    .placeholder(R.color.koi_light_gray)
                    .into(binding.teamLogo)
                binding.textImageStatus.text = "Imagen cargada"
            }
        }
    }

    private fun setupListeners() {
        // Seleccionar imagen de galería
        binding.buttonSelectLogo.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Seleccionar imagen por URL
        binding.buttonSelectLogoUrl.setOnClickListener {
            showImageUrlDialog()
        }

        binding.buttonRemoveLogo.setOnClickListener {
            imageUri = null
            binding.teamLogo.setImageResource(R.color.koi_light_gray)
            binding.textImageStatus.text = "Sin imagen"
            existingTeam = existingTeam?.copy(logo = "") ?: Team(logo = "")
        }
    }

    private fun showImageUrlDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_url, null)
        val editTextUrl = dialogView.findViewById<android.widget.EditText>(R.id.editTextImageUrl)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("URL del Logo")
            .setPositiveButton("Cargar") { _, _ ->
                val url = editTextUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadImageFromUrl(url)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadImageFromUrl(url: String) {
        binding.textImageStatus.text = "Cargando imagen..."

        Glide.with(requireContext())
            .load(url)
            .placeholder(R.color.koi_light_gray)
            .error(R.color.koi_light_gray)
            .into(binding.teamLogo)

        binding.teamLogo.postDelayed({
            binding.textImageStatus.text = "Imagen cargada"
            existingTeam = existingTeam?.copy(logo = url) ?: Team(logo = url)
        }, 1000)
    }

    private fun saveTeam() {
        val name = binding.editTextName.text.toString().trim()
        val game = binding.editTextGame.text.toString().trim() // Ahora del EditText
        val competition = binding.editTextCompetition.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val coach = binding.editTextCoach.text.toString().trim()

        if (name.isEmpty() || game.isEmpty() || competition.isEmpty()) {
            showToast("Completa nombre, juego y competición")
            return
        }

        // Determinar si estamos creando o editando
        val isEditing = !existingTeam?.id.isNullOrEmpty()

        val teamToSave = if (isEditing) {
            existingTeam!!.copy(
                name = name,
                game = game,
                competition = competition,
                description = description,
                coach = coach
            )
        } else {
            Team(
                name = name,
                game = game,
                competition = competition,
                description = description,
                coach = coach
            )
        }

        if (isEditing) {
            updateTeam(teamToSave)
        } else {
            createTeam(teamToSave)
        }
    }

    private fun createTeam(team: Team) {
        val newDocRef = db.collection("teams").document()
        val teamWithId = team.copy(id = newDocRef.id)

        newDocRef.set(teamWithId)
            .addOnSuccessListener {
                showToast("Equipo creado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error creando equipo: ${e.message}")
            }
    }

    private fun updateTeam(team: Team) {
        if (team.id.isEmpty()) {
            showToast("Error: ID de equipo inválido")
            return
        }

        db.collection("teams").document(team.id)
            .set(team)
            .addOnSuccessListener {
                showToast("Equipo actualizado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error actualizando equipo: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismissSafely() {
        if (isAdded) {
            dismiss()
        }
    }

    private fun notifyParentToReload() {
        (targetFragment as? ManageTeamsFragment)?.loadTeams()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}