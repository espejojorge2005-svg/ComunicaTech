package com.Jorge.asistentevoz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Esta función se activa mágicamente cuando te envían un mensaje
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.notification?.title ?: "Nueva Notificación"
        val cuerpo = message.notification?.body ?: "Tienes un mensaje nuevo en ComunicaTech"

        mostrarNotificacion(titulo, cuerpo)
    }

    // Esta función construye la tarjeta visual en el teléfono
    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "canal_comunicatech"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Ícono por defecto
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Los teléfonos modernos necesitan un "Canal" de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones Principales",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Mostramos la notificación con un ID aleatorio para que no se sobreescriban
        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    // Firebase le asigna un "Token" (DNI) a cada teléfono. Aquí lo atrapamos si cambia.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("Tu Token de dispositivo es: $token")
    }
}