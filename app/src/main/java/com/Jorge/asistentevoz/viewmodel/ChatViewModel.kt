package com.Jorge.asistentevoz.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// --- CRÍTICO: Firebase exige que los datos tengan valores por defecto (= "") ---
data class MensajeChat(
    val texto: String = "",
    val esMio: Boolean = false
)

data class ClaseGuardada(
    val id: Long = 0L,
    val nombre: String = "",
    val mensajes: List<MensajeChat> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val recognizedText = mutableStateOf("")
    val isRecording = mutableStateOf(false)
    val nombreClaseActual = mutableStateOf("Conversación sin título")

    // --- ESTADOS PARA LA VOZ ---
    val voiceSpeed = mutableFloatStateOf(1.0f)
    val voicePitch = mutableFloatStateOf(1.0f)

    val historialChat = mutableStateListOf<MensajeChat>()
    val clasesGuardadas = mutableStateListOf<ClaseGuardada>()

    // Estados para retroalimentación
    val isSaving = mutableStateOf(false)
    val errorMessage = mutableStateOf("")

    // Referencias
    private val sharedPreferences = application.getSharedPreferences("ChatPrefs", Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance() // LA CONEXIÓN A TU BASE DE DATOS

    init {
        cargarConfiguracionVoz()
        cargarClasesDesdeLaNube() // Al abrir la app, descarga los chats de ESTE usuario
    }

    // ========================================================
    // 1. CONFIGURACIÓN DE VOZ (Se queda en la memoria del teléfono)
    // ========================================================
    private fun cargarConfiguracionVoz() {
        voiceSpeed.floatValue = sharedPreferences.getFloat("voice_speed", 1.0f)
        voicePitch.floatValue = sharedPreferences.getFloat("voice_pitch", 1.0f)
    }

    fun updateVoiceSettings(speed: Float, pitch: Float) {
        voiceSpeed.floatValue = speed
        voicePitch.floatValue = pitch
        sharedPreferences.edit()
            .putFloat("voice_speed", speed)
            .putFloat("voice_pitch", pitch)
            .apply()
    }

    // ========================================================
    // 2. LÓGICA DEL CHAT EN LA NUBE (Privado por Usuario)
    // ========================================================
    fun toggleRecording() { isRecording.value = !isRecording.value }
    fun updateRecognizedText(text: String) { recognizedText.value = text }

    fun agregarMensaje(mensaje: MensajeChat) {
        historialChat.add(mensaje)
    }

    fun iniciarNuevaClase() {
        historialChat.clear()
        nombreClaseActual.value = "Conversación sin título"
    }

    // MAGIA: Guardar chat en la carpeta privada del usuario en Firebase
    fun guardarClaseActual(nombrePersonalizado: String) {
        if (historialChat.isEmpty()) return

        val currentUser = auth.currentUser ?: return // Si nadie inició sesión, se detiene

        val nombreFinal = nombrePersonalizado.ifBlank { "Clase ${clasesGuardadas.size + 1}" }
        val nuevaClase = ClaseGuardada(
            id = System.currentTimeMillis(),
            nombre = nombreFinal,
            mensajes = historialChat.toList()
        )

        isSaving.value = true
        errorMessage.value = ""

        // RUTA EN LA NUBE: users -> [ID_DEL_USUARIO] -> chats -> [ID_DEL_CHAT]
        db.collection("users").document(currentUser.uid)
            .collection("chats").document(nuevaClase.id.toString())
            .set(nuevaClase)
            .addOnSuccessListener {
                isSaving.value = false
                clasesGuardadas.add(0, nuevaClase)
                iniciarNuevaClase()
                Log.d("Firebase", "¡Chat guardado en la nube exitosamente!")
            }
            .addOnFailureListener { e ->
                isSaving.value = false
                errorMessage.value = "Error al guardar el chat: ${e.message}"
                Log.e("Firebase", "Error al guardar chat", e)
            }
    }

    fun cargarClase(clase: ClaseGuardada) {
        historialChat.clear()
        historialChat.addAll(clase.mensajes)
        nombreClaseActual.value = clase.nombre
    }

    // MAGIA: Eliminar chat de la nube
    fun eliminarClase(clase: ClaseGuardada) {
        val currentUser = auth.currentUser ?: return

        clasesGuardadas.remove(clase)
        if (nombreClaseActual.value == clase.nombre) {
            iniciarNuevaClase()
        }

        db.collection("users").document(currentUser.uid)
            .collection("chats").document(clase.id.toString())
            .delete()
    }

    // MAGIA: Descargar los chats de la nube al entrar a la app
    private fun cargarClasesDesdeLaNube() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Si no hay usuario, limpiamos todo por seguridad
            clasesGuardadas.clear()
            historialChat.clear()
            return
        }

        // Va a buscar solo los chats que le pertenecen a este usuario, ordenados del más nuevo al más viejo
        db.collection("users").document(currentUser.uid)
            .collection("chats")
            .orderBy("id", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val clasesCloud = mutableListOf<ClaseGuardada>()
                for (document in result) {
                    val clase = document.toObject(ClaseGuardada::class.java)
                    clasesCloud.add(clase)
                }
                clasesGuardadas.clear()
                clasesGuardadas.addAll(clasesCloud)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error al descargar chats", it)
            }
    }
}