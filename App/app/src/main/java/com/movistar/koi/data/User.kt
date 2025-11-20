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

    /**
     * Obtiene el rol del usuario actual
     */
    fun getUserRole(callback: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            callback(ROLE_USER) // Invitado = usuario normal
            return
        }

        // Si ya tenemos el rol en cache, usarlo
        if (currentUserRole != null) {
            callback(currentUserRole!!)
            return
        }

        // Obtener rol de Firestore
        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: ROLE_USER
                currentUserRole = role
                Log.d(TAG, "Rol obtenido: $role para ${user.email}")
                callback(role)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo rol: ${e.message}")
                callback(ROLE_USER) // Por defecto
            }
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    fun isAdmin(callback: (Boolean) -> Unit) {
        getUserRole { role ->
            callback(role == ROLE_ADMIN)
        }
    }

    /**
     * Limpia el cache (útil después de logout)
     */
    fun clearCache() {
        currentUserRole = null
    }
}