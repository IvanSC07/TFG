package com.movistar.koi.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.movistar.koi.data.News

class ReactionService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun addReaction(newsId: String, reactionType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("Usuario no autenticado")
            return
        }

        val userId = currentUser.uid

        db.runTransaction { transaction ->
            val newsRef = db.collection("news").document(newsId)
            val newsDoc = transaction.get(newsRef)

            if (!newsDoc.exists()) {
                throw Exception("La noticia no existe")
            }

            val news = newsDoc.toObject(News::class.java)
            val updatedReactions = news?.reactions?.toMutableMap() ?: mutableMapOf()
            val updatedUserReactions = news?.userReactions?.toMutableMap() ?: mutableMapOf()

            // Si el usuario ya tenía una reacción, la removemos
            val previousReaction = updatedUserReactions[userId]
            previousReaction?.let {
                updatedReactions[it] = (updatedReactions[it] ?: 1) - 1
                if (updatedReactions[it] == 0) {
                    updatedReactions.remove(it)
                }
            }

            // Añadir nueva reacción
            updatedReactions[reactionType] = (updatedReactions[reactionType] ?: 0) + 1
            updatedUserReactions[userId] = reactionType

            transaction.update(newsRef,
                "reactions", updatedReactions,
                "userReactions", updatedUserReactions
            )
        }.addOnSuccessListener {
            Log.d("ReactionService", "Reacción añadida exitosamente")
            onSuccess()
        }.addOnFailureListener { exception ->
            Log.e("ReactionService", "Error añadiendo reacción: ${exception.message}")
            onError(exception.message ?: "Error desconocido")
        }
    }

    fun getCurrentUserReaction(news: News): String? {
        val currentUser = auth.currentUser
        return currentUser?.uid?.let { userId ->
            news.userReactions[userId]
        }
    }
}