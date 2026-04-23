package com.Jorge.asistentevoz.navigation

import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Jorge.asistentevoz.ui.*
import com.google.firebase.auth.FirebaseAuth

/** Duración global de todas las transiciones de navegación (ms). */
private const val TRANSITION_DURATION = 400

/**
 * Composable de navegación central de la aplicación.
 *
 * Responsabilidad única: definir el grafo de rutas y conectar cada
 * destino con su Composable correspondiente.
 *
 * @param navController  Controlador de navegación creado en MainActivity.
 * @param startDestination Ruta inicial calculada (onboarding / login / home).
 * @param tts            Instancia de TextToSpeech inicializada en MainActivity.
 * @param sharedPreferences Preferencias necesarias para marcar el primer lanzamiento.
 * @param auth           Instancia de FirebaseAuth para leer el usuario actual.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    tts: TextToSpeech?,
    sharedPreferences: SharedPreferences,
    auth: FirebaseAuth
) {
    Scaffold { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),

            // ── Navegación hacia adelante ────────────────────────────────────
            // La nueva pantalla entra deslizándose desde la derecha + fadeIn.
            enterTransition = {
                slideIntoContainer(
                    towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            // La pantalla actual se desvanece mientras la nueva entra.
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION / 2)) +
                slideOutOfContainer(
                    towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(TRANSITION_DURATION),
                    targetOffset  = { it / 6 }   // salida sutil: solo 1/6 del ancho
                )
            },

            // ── Navegación hacia atrás (popBackStack) ────────────────────────
            // La pantalla anterior vuelve desde la izquierda + fadeIn.
            popEnterTransition = {
                slideIntoContainer(
                    towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            // La pantalla descartada sale hacia la derecha.
            popExitTransition = {
                slideOutOfContainer(
                    towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeOut(animationSpec = tween(TRANSITION_DURATION / 2))
            }
        ) {

            // ── Flujo de autenticación ──────────────────────────────────────────
            composable(AppRoutes.Onboarding.route) {
                OnboardingScreen(
                    onFinishTutorial = {
                        sharedPreferences.edit()
                            .putBoolean("is_first_launch", false)
                            .apply()
                        navController.navigate(AppRoutes.Login.route) {
                            popUpTo(AppRoutes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.Login.route) {
                LoginScreen(
                    onLoginSuccess = { _, _ ->
                        navController.navigate(AppRoutes.Home.route) {
                            popUpTo(AppRoutes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(AppRoutes.Register.route)
                    }
                )
            }

            composable(AppRoutes.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { _, _ ->
                        navController.navigate(AppRoutes.Home.route) {
                            popUpTo(AppRoutes.Register.route) { inclusive = true }
                            popUpTo(AppRoutes.Login.route)   { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // ── Pantallas principales ───────────────────────────────────────────
            composable(AppRoutes.Home.route) {
                val currentName = auth.currentUser?.displayName ?: "Usuario"
                val photoUrl    = auth.currentUser?.photoUrl?.toString() ?: ""
                HomeScreen(
                    nombreUsuario        = currentName,
                    photoUrl             = photoUrl,
                    onModoOyenteClick    = { navController.navigate(AppRoutes.Chat.route) },
                    onModoSordoClick     = { navController.navigate(AppRoutes.Senas.route) },
                    onSettingsClick      = { navController.navigate(AppRoutes.Settings.route) },
                    onNewsClick          = { navController.navigate(AppRoutes.News.route) },
                    onScannerClick       = { navController.navigate(AppRoutes.Scanner.route) },
                    onProfileClick       = { navController.navigate(AppRoutes.Profile.route) },
                    onConversationsClick = { navController.navigate(AppRoutes.Conversations.route) },
                    onEmergencyMapClick  = { navController.navigate(AppRoutes.EmergencyMap.route) }
                )
            }

            composable(AppRoutes.Chat.route) {
                ChatScreen(onBackClick = { navController.popBackStack() })
            }

            composable(AppRoutes.Senas.route) {
                SignLanguageScreen(tts = tts, onBackClick = { navController.popBackStack() })
            }

            composable(AppRoutes.News.route) {
                NewsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(AppRoutes.Profile.route) {
                ProfileScreen(onBackClick = { navController.popBackStack() })
            }

            composable(AppRoutes.EmergencyMap.route) {
                EmergencyMapScreen(onBackClick = { navController.popBackStack() })
            }

            composable(AppRoutes.Conversations.route) {
                ConversationsListScreen(
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { contactId ->
                        navController.navigate(AppRoutes.RemoteChat.createRoute(contactId))
                    }
                )
            }

            // Ruta con argumento dinámico: remote_chat/{contactId}
            composable(
                route = AppRoutes.RemoteChat.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoutes.RemoteChat.ARG_CONTACT_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments
                    ?.getString(AppRoutes.RemoteChat.ARG_CONTACT_ID) ?: ""
                RemoteChatScreen(
                    otherUserId = contactId,
                    tts         = tts,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.Scanner.route) {
                ScannerScreen(
                    tts         = tts,
                    onBackClick = { navController.popBackStack() },
                    onContactScanned = { contactId ->
                        navController.navigate(AppRoutes.RemoteChat.createRoute(contactId)) {
                            popUpTo(AppRoutes.Scanner.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.Settings.route) {
                SettingsScreen(
                    tts         = tts,
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = {
                        auth.signOut()
                        navController.navigate(AppRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
