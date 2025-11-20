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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUserInterface()
    }

//    private fun setupUserInterface() {
//        setupToolbar()
//        setupNavigation()
//        setupMenuBasedOnAuth()
//        requestNotificationPermission()
//        startMatchMonitoringWorker()
//
//        // Cargar fragmento inicial solo si es la primera vez
//        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
//            loadFragment(NewsFragment())
//        }
//
//        // Mostrar estado de autenticación
//        val user = auth.currentUser
//        if (user != null) {
//            Log.d(TAG, "✅ Usuario autenticado: ${user.email}")
//            Toast.makeText(this, "Bienvenido ${user.email}", Toast.LENGTH_SHORT).show()
//        } else {
//            Log.d(TAG, "🔓 Modo invitado activado")
//            Toast.makeText(this, "Modo invitado - Acceso de solo lectura", Toast.LENGTH_LONG).show()
//        }
//    }
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
    private fun checkUserRole() {
        UserManager.getUserRole { role ->
            isUserAdmin = role == UserManager.ROLE_ADMIN
            Log.d(TAG, "Usuario es admin: $isUserAdmin")

            // Actualizar el menú si es necesario
            invalidateOptionsMenu()

            // Mostrar mensaje si es admin
            if (isUserAdmin) {
                Toast.makeText(this, "Modo Administrador activado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Configura la toolbar con el menú de 3 puntos
     */
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar el título manualmente
        supportActionBar?.title = "MOVISTAR KOI"
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    /**
     * Configura el menú basado en la autenticación
     */
    private fun setupMenuBasedOnAuth() {
        // El menú se configura en onCreateOptionsMenu
    }

    /**
     * Configura la navegación inferior
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
     * Crea el menú de opciones (3 puntos)
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val user = auth.currentUser

        // Opciones de autenticación
        if (user != null) {
            menu.findItem(R.id.action_logout).isVisible = true
            menu.findItem(R.id.action_login).isVisible = false
        } else {
            menu.findItem(R.id.action_logout).isVisible = false
            menu.findItem(R.id.action_login).isVisible = true
        }

        // ✅ Opciones de administrador (solo para admins)
        menu.findItem(R.id.action_admin).isVisible = isUserAdmin

        return true
    }

    /**
     * Maneja las opciones del menú
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

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

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

    private fun performLogout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Log.d(TAG, "Logout exitoso")
                // Limpiar cache de usuario
                UserManager.clearCache()
                // Recargar la actividad para actualizar el menú
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error en logout: ${e.message}")
                Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

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

            Log.d(TAG, "🎯 Worker de monitoreo programado cada 15 minutos")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error programando worker: ${e.message}", e)
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "✅ Permisos de notificación concedidos")
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
                    Log.d(TAG, "📢 Solicitando permisos de notificación...")
                }
            }
        } else {
            Log.d(TAG, "✅ Android <13, no se necesitan permisos explícitos")
        }
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "✅ Permisos de notificación concedidos por el usuario")
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "❌ Permisos de notificación denegados por el usuario")
                    Toast.makeText(this, "Las notificaciones estarán desactivadas", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}