package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movistar.koi.adapters.AdminOptionsAdapter
import com.movistar.koi.data.UserManager
import com.movistar.koi.databinding.FragmentAdminPanelBinding
import com.movistar.koi.data.AdminOption

/**
 * Panel de administración
 */
class AdminPanelFragment : Fragment() {

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    private lateinit var adminAdapter: AdminOptionsAdapter

    /**
     * Constantes
     */
    companion object {
        private const val TAG = "AdminPanelFragment"
    }

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        checkAdminPermissions()
    }

    /**
     * Configura la interfaz de usuario
     */
    private fun setupUI() {
        adminAdapter = AdminOptionsAdapter { option ->
            when (option.id) {
                1 -> navigateToManageNews()
                2 -> navigateToManageMatches()
                3 -> navigateToManageTeams()
                4 -> navigateToManagePlayers()
                5 -> navigateToManageStreams()
                6 -> navigateToSendNotifications()
            }
        }

        binding.recyclerViewAdmin.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adminAdapter
            setHasFixedSize(true)
        }

        // Cargar opciones de administración
        loadAdminOptions()
    }

    /**
     * Verifica los permisos de administrador
     */
    private fun checkAdminPermissions() {
        Log.d(TAG, "Verificando permisos de administrador...")

        UserManager.isAdmin { isAdmin ->
            Log.d(TAG, "Resultado verificación admin: $isAdmin")

            if (!isAdmin) {
                Log.w(TAG, "Usuario sin permisos de admin")
                binding.statusText.text = "No tienes permisos de administrador"
                binding.statusText.visibility = View.VISIBLE
                binding.recyclerViewAdmin.visibility = View.GONE

                // Opcional: mostrar mensaje más informativo
                Toast.makeText(requireContext(), "Acceso restringido a administradores", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "Usuario es administrador, mostrando panel")
                binding.statusText.visibility = View.GONE
                binding.recyclerViewAdmin.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Carga las opciones de administración
     */
    private fun loadAdminOptions() {
        val adminOptions = listOf(
            AdminOption(
                id = 1,
                title = "Gestionar Noticias",
                description = "Crear, editar y eliminar noticias",
                iconRes = android.R.drawable.ic_menu_agenda,
                colorRes = com.movistar.koi.R.color.koi_purple
            ),
            AdminOption(
                id = 2,
                title = "Gestionar Partidos",
                description = "Programar partidos y actualizar resultados",
                iconRes = android.R.drawable.ic_menu_my_calendar,
                colorRes = com.movistar.koi.R.color.koi_light_blue
            ),
            AdminOption(
                id = 3,
                title = "Gestionar Equipos",
                description = "Administrar equipos y plantillas",
                iconRes = android.R.drawable.ic_menu_myplaces,
                colorRes = com.movistar.koi.R.color.koi_purple
            ),
            AdminOption(
                id = 4,
                title = "Gestionar Jugadores",
                description = "Administrar fichas de jugadores",
                iconRes = android.R.drawable.ic_menu_agenda,
                colorRes = com.movistar.koi.R.color.koi_light_blue
            ),
            AdminOption(
                id = 5,
                title = "Gestionar Streams",
                description = "Programar y gestionar streams en directo",
                iconRes = android.R.drawable.ic_media_play,
                colorRes = com.movistar.koi.R.color.koi_purple
            ),
            AdminOption(
                id = 6,
                title = "Enviar Notificaciones",
                description = "Enviar notificaciones push a los usuarios",
                iconRes = android.R.drawable.ic_dialog_info,
                colorRes = com.movistar.koi.R.color.koi_light_blue
            )
        )

        adminAdapter.updateOptions(adminOptions)
    }

    /**
     * Navega a la pantalla de gestión de noticias
     */
    private fun navigateToManageNews() {
        val fragment = ManageNewsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Navega a la pantalla de gestión de partidos
     */
    private fun navigateToManageMatches() {
        val fragment = ManageMatchesFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Navega a la pantalla de gestión de equipos
     */
    private fun navigateToManageTeams() {
        val fragment = ManageTeamsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Navega a la pantalla de gestión de jugadores
     */
    private fun navigateToManagePlayers() {
        val fragment = ManagePlayersFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Navega a la pantalla de gestión de streams
     */
    private fun navigateToManageStreams() {
        val fragment = ManageStreamsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Navega a la pantalla de envío de notificaciones
     */
    private fun navigateToSendNotifications() {
        val fragment = SendNotificationsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("admin_panel")
            .commit()
    }

    /**
     * Limpia los recursos
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}