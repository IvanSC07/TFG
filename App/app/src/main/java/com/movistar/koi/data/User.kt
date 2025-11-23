package com.movistar.koi.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserManager {
    private const val TAG = "UserManager"

    // Roles
    const val ROLE_USER = "user"
    const val ROLE_ADMIN = "admin"

    // Cache del rol del usuario
    private var currentUserRole: String? = null
    private var lastUserId: String? = null

    /**
     * Obtiene el rol del usuario actual
     */
    fun getUserRole(callback: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "Usuario no autenticado, rol: USER")
            callback(ROLE_USER)
            return
        }

        // Si el usuario cambió, limpiar cache
        if (lastUserId != user.uid) {
            currentUserRole = null
            lastUserId = user.uid
        }

        // Si ya tenemos el rol en cache y es el mismo usuario, usarlo
        if (currentUserRole != null) {
            Log.d(TAG, "Rol desde cache: $currentUserRole")
            callback(currentUserRole!!)
            return
        }

        // Obtener rol de Firestore
        Log.d(TAG, "Buscando rol en Firestore para UID: ${user.uid}")
        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: ROLE_USER
                    currentUserRole = role
                    Log.d(TAG, "✅ Rol obtenido de Firestore: $role para ${user.email}")
                    callback(role)
                } else {
                    // Si no existe el documento, crear uno por defecto
                    Log.w(TAG, "Documento de usuario no encontrado, creando uno...")
                    createDefaultUserDocument(user, callback)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error obteniendo rol: ${e.message}")
                callback(ROLE_USER) // Por defecto en caso de error
            }
    }

    private fun createDefaultUserDocument(user: com.google.firebase.auth.FirebaseUser, callback: (String) -> Unit) {
        val userData = hashMapOf<String, Any>(
            "email" to (user.email ?: ""),
            "role" to ROLE_USER,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Documento de usuario creado con rol USER")
                currentUserRole = ROLE_USER
                callback(ROLE_USER)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creando documento de usuario: ${e.message}")
                callback(ROLE_USER)
            }
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    fun isAdmin(callback: (Boolean) -> Unit) {
        getUserRole { role ->
            val isAdmin = role == ROLE_ADMIN
            Log.d(TAG, "Verificación admin: $isAdmin (rol: $role)")
            callback(isAdmin)
        }
    }

    /**
     * Limpia el cache (útil después de logout o cambio de usuario)
     */
    fun clearCache() {
        currentUserRole = null
        lastUserId = null
        Log.d(TAG, "Cache de usuario limpiado")
    }

    /**
     * Fuerza la actualización del rol desde Firestore
     */
    fun refreshUserRole(callback: (String) -> Unit) {
        currentUserRole = null
        lastUserId = null
        getUserRole(callback)
    }
}