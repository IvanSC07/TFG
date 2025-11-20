package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.movistar.koi.databinding.FragmentSendNotificationsBinding

class SendNotificationsFragment : Fragment() {

    private var _binding: FragmentSendNotificationsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "SendNotificationsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.title = "Enviar Notificaciones"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Configurar tipos de notificación
        setupNotificationTypes()

        // Configurar botón de envío
        binding.btnSendNotification.setOnClickListener {
            sendNotification()
        }
    }

    private fun setupNotificationTypes() {
        val notificationTypes = arrayOf(
            "Noticia importante",
            "Partido en directo",
            "Recordatorio de partido",
            "Actualización de equipo",
            "Stream especial",
            "Personalizada"
        )

        binding.spinnerNotificationType.adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            notificationTypes
        )
    }

    private fun sendNotification() {
        val title = binding.editTextTitle.text?.toString()?.trim() ?: ""
        val message = binding.editTextMessage.text?.toString()?.trim() ?: ""
        val notificationType = binding.spinnerNotificationType.selectedItem.toString()

        if (title.isEmpty() || message.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Completa título y mensaje", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar preview
        showNotificationPreview(title, message, notificationType)

        // TODO: Implementar envío real a Firebase Messaging
        android.widget.Toast.makeText(requireContext(), "Enviando notificación...", android.widget.Toast.LENGTH_SHORT).show()

        // Simular envío
        binding.progressBar.visibility = View.VISIBLE
        android.os.Handler().postDelayed({
            binding.progressBar.visibility = View.GONE
            showSendResult(true, "Notificación enviada a todos los usuarios")

            // Limpiar formulario
            binding.editTextTitle.text?.clear()
            binding.editTextMessage.text?.clear()
        }, 2000)
    }

    private fun showNotificationPreview(title: String, message: String, type: String) {
        binding.previewTitle.text = title
        binding.previewMessage.text = message
        binding.previewType.text = "Tipo: $type"
        binding.previewCard.visibility = View.VISIBLE
    }

    private fun showSendResult(success: Boolean, message: String) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle(if (success) "✅ Éxito" else "❌ Error")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)

        if (success) {
            dialog.setIcon(android.R.drawable.ic_dialog_info)
        } else {
            dialog.setIcon(android.R.drawable.ic_dialog_alert)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}