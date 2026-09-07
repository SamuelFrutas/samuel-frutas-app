plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "br.com.samuelfrutas.app"; compileSdk = 35
    defaultConfig { applicationId = "br.com.samuelfrutas.app"; minSdk = 23; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
