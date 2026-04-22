plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.Jorge.asistentevoz"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.Jorge.asistentevoz"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Firebase (Agregado por el asistente)
    implementation(libs.firebase.firestore)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Iconos extendidos para la interfaz de accesibilidad
    implementation("androidx.compose.material:material-icons-extended")

    // Ciclo de vida y ViewModel para Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navegación entre pantallas
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- LIBRERÍAS DE COIL (NUEVAS) ---
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Librerías de Firebase para Login
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage-ktx")
    // Librerías para conectarse a APIs (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Librería para recibir Notificaciones Push
    implementation("com.google.firebase:firebase-messaging")

    // --- LIBRERÍAS DE CAMERAX (NUEVAS) ---
    val camerax_version = "1.3.2"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    
    // --- LIBRERÍA ML KIT BARCODE SCANNING (NUEVA) ---
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // --- LIBRERÍA ZXING PARA GENERAR CÓDIGOS QR (NUEVA) ---
    implementation("com.google.zxing:core:3.5.3")

    // --- LIBRERÍAS DE GOOGLE MAPS COMPOSE (NUEVAS) ---
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
}