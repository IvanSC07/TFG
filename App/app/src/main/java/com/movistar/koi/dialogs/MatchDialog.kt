package com.movistar.koi.dialogs

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.movistar.koi.ManageMatchesFragment
import com.movistar.koi.R
import com.movistar.koi.data.Match
import com.movistar.koi.data.Team
import com.movistar.koi.databinding.DialogMatchBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialogo para crear o editar un partido
 */
class MatchDialog : DialogFragment() {

    private var _binding: DialogMatchBinding? = null
    private val binding get() = _binding!!
    private var existingMatch: Match? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Callback para notificar que el partido se ha guardado
     */
    private var onMatchSavedListener: (() -> Unit)? = null

    /**
     * Calendario para seleccionar la fecha y hora
     */
    private var selectedCalendar: Calendar = Calendar.getInstance()

    /**
     * Abre la galería para seleccionar una imagen
     */
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebaseStorage(it)
        }
    }

    /**
     * Instancia del diálogo
     */
    companion object {
        private const val TAG = "MatchDialog"

        /**
         * Crea una nueva instancia del diálogo
         * @param match Partido existente para editar, si es nulo se crea uno nuevo
         */
        fun newInstance(match: Match? = null): MatchDialog {
            val dialog = MatchDialog()
            match?.let {
                val args = Bundle()
                args.putString("id", it.id)
                args.putLong("date", it.date.time)
                args.putString("opponent", it.opponent)
                args.putString("competition", it.competition)
                args.putString("result", it.result)
                args.putString("status", it.status)
                args.putString("team", it.team)
                args.putString("opponentLogo", it.opponentLogo)
                args.putString("streamUrl", it.streamUrl)
                dialog.arguments = args
            }
            return dialog
        }
    }

    /**
     * Crea el diálogo
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMatchBinding.inflate(LayoutInflater.from(requireContext()))

        // Configurar dropdowns
        setupDropdowns()

        // Cargar equipos desde Firestore
        loadTeamsFromFirestore()

        // Cargar datos existentes si estamos editando
        loadExistingData()

        // Configurar listeners
        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingMatch == null) "Crear Partido" else "Editar Partido")
            .setPositiveButton("Guardar") { _, _ ->
                binding.root.post { saveMatch() }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Configura los dropdowns de equipos y estados del partido
     */
    private fun setupDropdowns() {
        // Estados del partido
        val statuses = arrayOf("scheduled", "live", "finished")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
    }

    /**
     * Carga equipos desde Firestore
     */
    private fun loadTeamsFromFirestore() {
        FirebaseFirestore.getInstance().collection("teams")
            .get()
            .addOnSuccessListener { documents ->
                val teamNames = mutableListOf<String>()

                for (document in documents) {
                    try {
                        val team = document.toObject(Team::class.java)
                        teamNames.add(team.game)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo equipo: ${e.message}")
                    }
                }

                // Si no hay equipos, usar valores por defecto
                if (teamNames.isEmpty()) {
                    teamNames.addAll(arrayOf("VALORANT", "League of Legends", "Pokémon", "TFT", "Call of Duty"))
                }

                val teamAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teamNames)
                teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTeam.adapter = teamAdapter

                // Seleccionar equipo existente si estamos editando
                existingMatch?.team?.let { teamName ->
                    val position = teamAdapter.getPosition(teamName)
                    if (position >= 0) {
                        binding.spinnerTeam.setSelection(position)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error cargando equipos:", exception)
                // Fallback a valores por defecto
                val defaultTeams = arrayOf("VALORANT", "League of Legends", "Pokémon", "TFT", "Call Of Duty")
                val teamAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, defaultTeams)
                teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTeam.adapter = teamAdapter
            }
    }

    /**
     * Carga datos existentes del partido
     */
    private fun loadExistingData() {
        arguments?.let { args ->
            val matchId = args.getString("id") ?: ""

            existingMatch = Match(
                id = matchId,
                date = Date(args.getLong("date", Date().time)),
                opponent = args.getString("opponent") ?: "",
                competition = args.getString("competition") ?: "",
                result = args.getString("result") ?: "",
                status = args.getString("status") ?: "scheduled",
                team = args.getString("team") ?: "",
                opponentLogo = args.getString("opponentLogo") ?: "",
                streamUrl = args.getString("streamUrl") ?: ""
            )

            // Llenar los campos
            binding.editTextOpponent.setText(existingMatch?.opponent)
            binding.editTextCompetition.setText(existingMatch?.competition)
            binding.editTextResult.setText(existingMatch?.result)
            binding.editTextOpponentLogo.setText(existingMatch?.opponentLogo)
            binding.editTextStreamUrl.setText(existingMatch?.streamUrl)

            // Configurar fecha y hora
            val calendar = Calendar.getInstance().apply {
                time = existingMatch?.date ?: Date()
            }
            selectedCalendar = calendar
            updateDateAndTimeViews()

            // Seleccionar estado
            selectSpinnerValue(binding.spinnerStatus, existingMatch?.status ?: "scheduled")

            // Cargar imagen del oponente si existe
            if (!existingMatch?.opponentLogo.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(existingMatch?.opponentLogo)
                    .placeholder(R.color.koi_light_gray)
                    .into(binding.imageViewOpponentLogo)
            }
        }
    }


    /**
     * Selecciona un valor en un Spinner
     */
    private fun selectSpinnerValue(spinner: android.widget.Spinner, value: String) {
        val adapter = spinner.adapter as ArrayAdapter<*>
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                spinner.setSelection(i)
                break
            }
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupListeners() {
        binding.buttonSelectLogo.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonRemoveLogo.setOnClickListener {
            binding.imageViewOpponentLogo.setImageResource(R.color.koi_light_gray)
            binding.editTextOpponentLogo.setText("")
        }

        binding.editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.editTextTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    /**
     * Muestra el diálogo de selección de fecha y hora
     */
    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateAndTimeViews()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * Muestra el diálogo de selección de hora
     */
    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                updateDateAndTimeViews()
            },
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    /**
     * Actualiza los campos de fecha y hora en el diálogo
     */
    private fun updateDateAndTimeViews() {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.editTextDate.setText(dateFormat.format(selectedCalendar.time))
        binding.editTextTime.setText(timeFormat.format(selectedCalendar.time))
    }

    /**
     * Subir imagen a Firebase Storage
     */
    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val filename = "opponent_logos/${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child(filename)

        binding.buttonSelectLogo.isEnabled = false
        binding.buttonSelectLogo.text = "Subiendo..."

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    binding.editTextOpponentLogo.setText(imageUrl)
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.color.koi_light_gray)
                        .into(binding.imageViewOpponentLogo)

                    binding.buttonSelectLogo.isEnabled = true
                    binding.buttonSelectLogo.text = "Seleccionar Logo"
                    showToast("Logo subido correctamente")
                }
            }
            .addOnFailureListener { e ->
                binding.buttonSelectLogo.isEnabled = true
                binding.buttonSelectLogo.text = "Seleccionar Logo"
                showToast("Error subiendo logo: ${e.message}")
            }
    }

    /**
     * Guarda el partido
     */
    private fun saveMatch() {
        val opponent = binding.editTextOpponent.text.toString().trim()
        val competition = binding.editTextCompetition.text.toString().trim()
        val result = binding.editTextResult.text.toString().trim()
        val opponentLogo = binding.editTextOpponentLogo.text.toString().trim()
        val streamUrl = binding.editTextStreamUrl.text.toString().trim()

        val status = binding.spinnerStatus.selectedItem.toString()
        val team = binding.spinnerTeam.selectedItem.toString()

        // Obtener fecha y hora desde selectedCalendar
        val date = selectedCalendar.time

        if (opponent.isEmpty() || competition.isEmpty()) {
            showToast("Completa oponente y competición")
            return
        }

        val isEditing = !existingMatch?.id.isNullOrEmpty()

        val matchToSave = if (isEditing) {
            existingMatch!!.copy(
                opponent = opponent,
                competition = competition,
                result = result,
                status = status,
                team = team,
                opponentLogo = opponentLogo,
                streamUrl = streamUrl,
                date = date
            )
        } else {
            Match(
                date = date,
                opponent = opponent,
                competition = competition,
                result = result,
                status = status,
                team = team,
                opponentLogo = opponentLogo,
                streamUrl = streamUrl
            )
        }

        if (isEditing) {
            updateMatch(matchToSave)
        } else {
            createMatch(matchToSave)
        }
    }

    /**
     * Crea un partido
     */
    private fun createMatch(match: Match) {
        val newDocRef = db.collection("matches").document()
        val matchWithId = match.copy(id = newDocRef.id)

        newDocRef.set(matchWithId)
            .addOnSuccessListener {
                showToast("Partido creado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error creando partido: ${e.message}")
            }
    }

    /**
     * Actualiza un partido
     */
    private fun updateMatch(match: Match) {
        if (match.id.isEmpty()) {
            showToast("Error: ID de partido inválido")
            return
        }

        db.collection("matches").document(match.id)
            .set(match)
            .addOnSuccessListener {
                showToast("Partido actualizado exitosamente")
                dismissSafely()
                notifyParentToReload()
            }
            .addOnFailureListener { e ->
                showToast("Error actualizando partido: ${e.message}")
            }
    }

    /**
     * Notifica al padre que se debe recargar la lista de partidos
     */
    private fun notifyParentToReload() {
        (targetFragment as? ManageMatchesFragment)?.loadMatches()
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
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onMatchSavedListener = null
    }
}