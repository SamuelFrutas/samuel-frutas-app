package br.com.samuelfrutas.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Firebase configuration shared with the existing Samuel Frutas Pedidos web system.
 * The Pedidos panel uses the `samuelfrutasbot` Firebase project.
 */
object FirebaseConfig {
    private const val API_KEY = "AIzaSyB1E9oQsIwYO2-r4W5-uqK4ax92OmxISOI"
    private const val APPLICATION_ID = "1:1052699327049:web:cf22d68c76d064d56b5d98"
    private const val PROJECT_ID = "samuelfrutasbot"
    private const val STORAGE_BUCKET = "samuelfrutasbot.firebasestorage.app"
    private const val MESSAGING_SENDER_ID = "1052699327049"

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
