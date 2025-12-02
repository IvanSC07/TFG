package com.movistar.koi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.movistar.koi.data.UserManager
import com.movistar.koi.databinding.ActivityMainBinding
import com.movistar.koi.workers.MatchMonitorWorker
import java.util.concurrent.TimeUnit

/**
 * Actividad principal
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private var isUserAdmin = false

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    /**
     * Crea la vista
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUserInterface()
    }

    /**
     * Configura la interfaz de usuario
     */
        private fun setupUserInterface() {
            setupToolbar()
            setupNavigation()
            checkUserRole()
            requestNotificationPermission()
            startMatchMonitoringWorker()

            if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                loadFragment(NewsFragment())
            }
        }

    /**
     * Comprueba el rol del usuario
     */
    private fun checkUserRole() {
        UserManager.getUserRole { role ->
            isUserAdmin = role == UserManager.ROLE_ADMIN
            Log.d(TAG, "Usuario es admin: $isUserAdmin")

            invalidateOptionsMenu()

            // Mostrar mensaje si es admin
            if (isUserAdmin) {
                Toast.makeText(this, "Modo Administrador activado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Configura la barra de herramientas
     */
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar el título manualmente
        supportActionBar?.title = "MOVISTAR KOI"
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    /**
     * Configura la navegación
     */
    private fun setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_news -> {
                    loadFragment(NewsFragment())
                    true
                }
                R.id.nav_matches -> {
                    loadFragment(MatchesFragment())
                    true
                }
                R.id.nav_teams -> {
                    loadFragment(TeamsFragment())
                    true
                }
                R.id.nav_stream -> {
                    loadFragment(StreamFragment())
                    true
                }
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Crea el menú de opciones
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val user = auth.currentUser

        if (user != null) {
            menu.findItem(R.id.action_logout).isVisible = true
            menu.findItem(R.id.action_login).isVisible = false
        } else {
            menu.findItem(R.id.action_logout).isVisible = false
            menu.findItem(R.id.action_login).isVisible = true
        }

        menu.findItem(R.id.action_admin).isVisible = isUserAdmin

        return true
    }

    /**
     * Maneja la selección de elementos del menú
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                loadFragment(SettingsFragment())
                true
            }
            R.id.action_admin -> {
                loadFragment(AdminPanelFragment())
                true
            }
            R.id.action_login -> {
                goToLogin()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Navega a la pantalla de login
     */
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Muestra la confirmación de cierre de sesión
     */
    private fun showLogoutConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Realiza el cierre de sesión
     */
    private fun performLogout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Log.d(TAG, "Logout exitoso")
                // Limpiar cache de usuario
                UserManager.clearCache()
                // Redirigir a LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error en logout: ${e.message}")
                Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Carga un fragmento
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Programa el worker de monitoreo de partidos
     */
    private fun startMatchMonitoringWorker() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MatchMonitorWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "match_monitoring_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Worker de monitoreo programado cada 15 minutos")

        } catch (e: Exception) {
            Log.e(TAG, "Error programando worker: ${e.message}", e)
        }
    }

    /**
     * Solicita los permisos de notificación
     */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Permisos de notificación concedidos")
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionExplanation()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                    )
                    Log.d(TAG, "Solicitando permisos de notificación...")
                }
            }
        } else {
            Log.d(TAG, "Android <13, no se necesitan permisos explícitos")
        }
    }

    /**
     * Muestra la explicación de los permisos
     */
    private fun showPermissionExplanation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permisos de Notificación")
            .setMessage("Las notificaciones son necesarias para recibir alertas de partidos en directo, noticias importantes y actualizaciones del equipo.")
            .setPositiveButton("Activar") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Más tarde") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Maneja la respuesta de los permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permisos de notificación concedidos por el usuario")
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "Permisos de notificación denegados por el usuario")
                    Toast.makeText(this, "Las notificaciones estarán desactivadas", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}