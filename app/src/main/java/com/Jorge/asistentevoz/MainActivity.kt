package com.Jorge.asistentevoz

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Jorge.asistentevoz.ui.*
import com.Jorge.asistentevoz.ui.theme.AsistenteVozTheme
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        val currentUser = auth.currentUser

        val startDest = when {
            isFirstLaunch -> "onboarding"
            currentUser == null -> "login"
            else -> "home"
        }

        setContent {
            AsistenteVozTheme {
                val context = LocalContext.current
                var tts by remember { mutableStateOf<TextToSpeech?>(null) }
                val navController = rememberNavController()

                // --- MAGIA PARA PEDIR PERMISO DE NOTIFICACIONES (Android 13+) ---
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            println("¡Permiso de notificaciones concedido!")
                        } else {
                            println("El usuario denegó las notificaciones.")
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // -----------------------------------------------------------------

                DisposableEffect(context) {
                    val textToSpeech = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            tts?.language = Locale("es", "PE")
                        }
                    }
                    tts = textToSpeech
                    onDispose {
                        textToSpeech.stop()
                        textToSpeech.shutdown()
                    }
                }

                Scaffold { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinishTutorial = {
                                    sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { nombre, photoUrl ->
                                    sharedPreferences.edit()
                                        .putString("user_name", nombre)
                                        .putString("photo_url", photoUrl ?: "")
                                        .apply()
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = { nombre, photoUrl ->
                                    sharedPreferences.edit()
                                        .putString("user_name", nombre)
                                        .putString("photo_url", photoUrl ?: "")
                                        .apply()
                                    navController.navigate("home") {
                                        popUpTo("register") { inclusive = true }
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = { navController.popBackStack() }
                            )
                        }

                        composable("home") {
                            val currentName = sharedPreferences.getString("user_name", "Usuario") ?: "Usuario"
                            val photoUrl = sharedPreferences.getString("photo_url", "") ?: ""
                            HomeScreen(
                                nombreUsuario = currentName,
                                photoUrl = photoUrl,
                                onModoOyenteClick = { navController.navigate("chat") },
                                onModoSordoClick = { navController.navigate("senas") },
                                onSettingsClick = { navController.navigate("settings") },
                                onNewsClick = { navController.navigate("news") },
                                onScannerClick = { navController.navigate("scanner") },
                                onProfileClick = { navController.navigate("profile") },
                                onConversationsClick = { navController.navigate("conversations") },
                                onEmergencyMapClick = { navController.navigate("emergency_map") }
                            )
                        }

                        composable("chat") { ChatScreen(onBackClick = { navController.popBackStack() }) }
                        composable("senas") { SignLanguageScreen(tts = tts, onBackClick = { navController.popBackStack() }) }
                        composable("news") { NewsScreen(onBackClick = { navController.popBackStack() }) }
                        composable("profile") { ProfileScreen(onBackClick = { navController.popBackStack() }) }
                        composable("emergency_map") { EmergencyMapScreen(onBackClick = { navController.popBackStack() }) }
                        composable("conversations") { 
                            ConversationsListScreen(
                                onBackClick = { navController.popBackStack() },
                                onChatClick = { contactId -> navController.navigate("remote_chat/$contactId") }
                            ) 
                        }
                        composable(
                            "remote_chat/{contactId}",
                            arguments = listOf(androidx.navigation.navArgument("contactId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                            RemoteChatScreen(
                                otherUserId = contactId,
                                tts = tts,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("scanner") { 
                            ScannerScreen(
                                tts = tts, 
                                onBackClick = { navController.popBackStack() },
                                onContactScanned = { contactId ->
                                    navController.navigate("remote_chat/$contactId") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                }
                            ) 
                        }

                        composable("settings") {
                            SettingsScreen(
                                tts = tts,
                                onBackClick = { navController.popBackStack() },
                                onLogoutClick = {
                                    auth.signOut()
                                    sharedPreferences.edit().remove("user_name").apply()
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}