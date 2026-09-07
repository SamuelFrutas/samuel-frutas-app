package br.com.samuelfrutas.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseConfig {
    private const val API_KEY = "AIzaSyB1E9QsIwYO2-r4W5-uqK4ax92OmxISOI"
    private const val APPLICATION_ID = "1:1052699327049:web:cf22d68c76d064d56b5d98"
    private const val PROJECT_ID = "samuelfrutasbot"
    private const val STORAGE_BUCKET = "samuelfrutasbot.firebasestorage.app"

    fun initialize(context: Context): FirebaseApp {
        return FirebaseApp.getApps(context).firstOrNull()
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(APPLICATION_ID)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .build()
            )
            ?: error("Não foi possível inicializar o Firebase")
    }
}
