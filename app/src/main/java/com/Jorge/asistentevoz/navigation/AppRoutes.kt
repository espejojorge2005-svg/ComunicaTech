package com.Jorge.asistentevoz.navigation

/**
 * Rutas de navegación tipadas (Type-Safe Navigation).
 * Elimina los strings hardcodeados y centraliza toda la definición de rutas.
 *
 * Uso: navController.navigate(AppRoutes.Home.route)
 */
sealed class AppRoutes(val route: String) {

    // ── Flujo de autenticación ──────────────────────────────────────────────
    data object Onboarding : AppRoutes("onboarding")
    data object Login      : AppRoutes("login")
    data object Register   : AppRoutes("register")

    // ── Pantallas principales ───────────────────────────────────────────────
    data object Home          : AppRoutes("home")
    data object Chat          : AppRoutes("chat")
    data object Senas         : AppRoutes("senas")
    data object News          : AppRoutes("news")
    data object Profile       : AppRoutes("profile")
    data object Scanner       : AppRoutes("scanner")
    data object EmergencyMap  : AppRoutes("emergency_map")
    data object Conversations : AppRoutes("conversations")
    data object Settings      : AppRoutes("settings")

    // ── Ruta con argumento dinámico ─────────────────────────────────────────
    /**
     * Ruta del chat remoto P2P.
     *
     * Definición de la ruta en el NavHost:  [routeWithArg]
     * Navegación:                           [createRoute]
     *
     * Ejemplo:
     *   navController.navigate(AppRoutes.RemoteChat.createRoute(contactId))
     */
    data object RemoteChat : AppRoutes("remote_chat/{contactId}") {

        /** Argumento esperado por el NavHost. */
        const val ARG_CONTACT_ID = "contactId"

        /** Ruta con el marcador {contactId} para registrar en el NavHost. */
        val routeWithArg: String get() = route

        /** Construye la ruta concreta reemplazando el argumento. */
        fun createRoute(contactId: String): String = "remote_chat/$contactId"
    }
}
