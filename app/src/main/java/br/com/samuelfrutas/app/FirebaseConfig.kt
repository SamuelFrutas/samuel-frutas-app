package br.com.samuelfrutas.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Firebase configuration shared with the existing Samuel Frutas web system.
 * The web admin authenticates against the `samuel-frutas` Firebase project,
 * so the native app must use that same project for Auth and Firestore.
 */
object FirebaseConfig {
    private const val API_KEY = "AIzaSyBLU3UNXuPGUFrpmV6syI80ynUHppupeNA"
    private const val APPLICATION_ID = "1:475005081261:web:314ac91f8b0578b995824"
    private const val PROJECT_ID = "samuel-frutas"
    private const val STORAGE_BUCKET = "samuel-frutas.firebasestorage.app"
    private const val MESSAGING_SENDER_ID = "475005081261"

    fun initialize(context: Context): FirebaseApp {
        return FirebaseApp.getApps(context).firstOrNull()
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(APPLICATION_ID)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(MESSAGING_SENDER_ID)
                    .build()
            )
            ?: error("Não foi possível inicializar o Firebase")
    }
}
