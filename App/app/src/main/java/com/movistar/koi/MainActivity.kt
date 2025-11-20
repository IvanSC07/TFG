package com.movistar.koi

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.movistar.koi.databinding.ActivityMainBinding
import com.movistar.koi.workers.MatchMonitorWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar y navigation
        setupToolbar()
        setupNavigation()

        // Solicitar permisos de notificación
        requestNotificationPermission()

        // Iniciar worker
        startMatchMonitoringWorker()

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(NewsFragment())
        }
    }

    /**
     * Configura la toolbar con el menú de 3 puntos
     */
    private fun setupToolbar() {
        toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Quitar el título por defecto
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    /**
     * Inicia el worker de monitoreo de partidos
     */
    private fun startMatchMonitoringWorker() {
        try {
            // Crear restricciones (opcional, puedes quitarlas si quieres)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Crear trabajo periódico (cada 15 minutos con flexibilidad de 5 minutos)
            val workRequest: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<MatchMonitorWorker>(
                    repeatInterval = 15,
                    repeatIntervalTimeUnit = TimeUnit.MINUTES,
                    flexTimeInterval = 5,
                    flexTimeIntervalUnit = TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

            // Programar el trabajo
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

    /**
     * Solicita permisos de notificación (Android 13+)
     */
    private fun requestNotificationPermission() {
        // Solo necesario en Android 13 (API 33) y superior
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                    Log.d(TAG, "✅ Permisos de notificación concedidos")
                }

                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario ya denegó el permiso, mostrar explicación
                    showPermissionExplanation()
                }

                else -> {
                    // Solicitar permiso por primera vez
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                    )
                    Log.d(TAG, "📢 Solicitando permisos de notificación...")
                }
            }
        } else {
            // En versiones anteriores no se necesitan permisos explícitos
            Log.d(TAG, "✅ Android <13, no se necesitan permisos explícitos")
        }
    }

    /**
     * Maneja la respuesta de la solicitud de permisos
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
                    Log.d(TAG, "✅ Permisos de notificación concedidos por el usuario")
                    android.widget.Toast.makeText(this, "Notificaciones activadas", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "❌ Permisos de notificación denegados por el usuario")
                    android.widget.Toast.makeText(this, "Las notificaciones estarán desactivadas", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Muestra explicación de por qué necesitamos el permiso
     */
    private fun showPermissionExplanation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permisos de Notificación")
            .setMessage("Las notificaciones son necesarias para recibir alertas de partidos en directo, noticias importantes y actualizaciones del equipo.")
            .setPositiveButton("Activar") { _, _ ->
                // Intentar solicitar permiso nuevamente
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
     * Configura la navegación inferior
     */
    private fun setupNavigation() {
        bottomNavigationView = binding.bottomNavigationView

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
        return true
    }

    /**
     * Maneja las opciones del menú
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navegar a ajustes
                loadFragment(SettingsFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// Fragmento placeholder simplificado
class SimplePlaceholderFragment(private val title: String, private val message: String) : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = inflater.inflate(com.movistar.koi.R.layout.fragment_placeholder, container, false)

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<android.widget.TextView>(com.movistar.koi.R.id.sectionTitle).text = title
        view.findViewById<android.widget.TextView>(com.movistar.koi.R.id.sectionMessage).text = message
    }
}