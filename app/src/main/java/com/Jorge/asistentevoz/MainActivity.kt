package com.Jorge.asistentevoz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.Jorge.asistentevoz.navigation.AppNavigation
import com.Jorge.asistentevoz.navigation.AppRoutes
import com.Jorge.asistentevoz.ui.theme.AsistenteVozTheme
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

/**
 * Punto de entrada de la aplicación.
 *
 * Responsabilidades:
 *  1. Inicializar dependencias de ciclo de vida (FirebaseAuth, TTS, SplashScreen).
 *  2. Gestionar el permiso de notificaciones con UX amigable.
 *  3. Delegar todo el grafo de navegación a [AppNavigation].
 */
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch     = sharedPreferences.getBoolean("is_first_launch", true)

        // Destino inicial: calculado una sola vez, fuera del árbol Compose.
        val startDestination = when {
            isFirstLaunch            -> AppRoutes.Onboarding.route
            auth.currentUser == null -> AppRoutes.Login.route
            else                     -> AppRoutes.Home.route
        }

        setContent {
            AsistenteVozTheme {
                val context       = LocalContext.current
                val navController = rememberNavController()

                // ── TextToSpeech ────────────────────────────────────────────────
                var tts by remember { mutableStateOf<TextToSpeech?>(null) }
                DisposableEffect(context) {
                    val engine = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            tts?.language = Locale("es", "PE")
                        }
                    }
                    tts = engine
                    onDispose {
                        engine.stop()
                        engine.shutdown()
                    }
                }

                // ── Permiso de Notificaciones (Android 13+) ─────────────────────
                var showPermissionDialog by remember { mutableStateOf(false) }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) println("¡Permiso de notificaciones concedido!")
                        else          println("El usuario denegó las notificaciones.")
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val alreadyGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!alreadyGranted) showPermissionDialog = true
                    }
                }

                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = "Activa las Notificaciones",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            Text(
                                text = "Para mantenerte conectado, necesitamos enviarte notificaciones de nuevos mensajes.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showPermissionDialog = false
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPermissionDialog = false }) {
                                Text("Ahora no")
                            }
                        }
                    )
                }
                // ────────────────────────────────────────────────────────────────

                // ── Grafo de navegación (responsabilidad delegada) ───────────────
                AppNavigation(
                    navController      = navController,
                    startDestination   = startDestination,
                    tts                = tts,
                    sharedPreferences  = sharedPreferences,
                    auth               = auth
                )
            }
        }
    }
}