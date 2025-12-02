package com.movistar.koi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.movistar.koi.data.UserManager
import com.movistar.koi.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        setupUI()
        checkCurrentUser()
    }

    private fun setupUI() {
        // Botón de login manual
        binding.btnLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithEmail(email, password)
        }

        // Botón de registro
        binding.btnRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerWithEmail(email, password)
        }

        // Opción de continuar como invitado
        binding.btnGuest.setOnClickListener {
            continueAsGuest()
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Login exitoso: ${user?.email}")

                    // Limpiar cache antes de actualizar
                    UserManager.clearCache()

                    updateUserInFirestore(user)
                    goToMainActivity()
                } else {
                    Log.e(TAG, "Error en login: ${task.exception?.message}")
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerWithEmail(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Registro exitoso: ${user?.email}")

                    val adminEmails = listOf(
                        "admin@movistarkoi.com",
                        "sobrinocalzado2001ivan76@gmail.com",
                        "superadmin@koi.com"
                    )

                    val role = if (adminEmails.contains(email.lowercase())) "admin" else "user"

                    updateUserInFirestore(user, role, isNewUser = true)
                    goToMainActivity()
                } else {
                    Log.e(TAG, "Error en registro: ${task.exception?.message}")
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUserInFirestore(user: FirebaseUser?, role: String? = null, isNewUser: Boolean = false) {
        if (user == null) return

        // Si no se proporciona rol, obtenerlo del usuario existente o determinar
        val finalRole = if (role != null) {
            role
        } else {
            if (isNewUser) {
                val adminEmails = listOf(
                    "admin@movistarkoi.com",
                    "sobrinocalzado2001ivan76@gmail.com",
                    "superadmin@koi.com"
                )
                if (adminEmails.contains(user.email?.lowercase())) "admin" else "user"
            } else {
                null // No cambiar el rol existente
            }
        }

        val userData = hashMapOf<String, Any>(
            "email" to (user.email ?: ""),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )

        // Solo agregar rol si se está definiendo
        finalRole?.let {
            userData["role"] = it
            Log.d(TAG, "Definiendo rol: $it para ${user.email}")
        }

        if (isNewUser) {
            userData["createdAt"] = com.google.firebase.Timestamp.now()
        }

        db.collection("users")
            .document(user.uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Usuario actualizado en Firestore: ${user.email} - Rol: ${finalRole ?: "mantenido"}")
                if (finalRole == "admin") {
                    Toast.makeText(this, "Modo Administrador activado", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error actualizando usuario: ${e.message}")
            }
    }
    private fun checkCurrentUser() {
        if (auth.currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: ${auth.currentUser?.email}")
            goToMainActivity()
        }
    }

    private fun continueAsGuest() {
        Log.d(TAG, "Continuando como invitado")
        goToMainActivity()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}