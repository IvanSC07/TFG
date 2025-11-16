package com.movistar.koi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseConfig {

    // Instancia de Firestore para la base de datos
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Instancia de Authentication para manejar usuarios
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Instancia de Storage para almacenar archivos (imágenes)
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Referencias directas a las colecciones de Firestore
    val newsCollection = db.collection("news")
    val matchesCollection = db.collection("matches")
    val teamsCollection = db.collection("teams")
    val playersCollection = db.collection("players")
    val streamsCollection = db.collection("streams")
}