package com.Jorge.asistentevoz.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

data class RemoteMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class RemoteChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentUserName = auth.currentUser?.displayName ?: "Usuario"
    
    val messages = mutableStateListOf<RemoteMessage>()
    val otherUserName = mutableStateOf("Desconocido")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")

    private var currentChatId: String = ""

    fun startOrJoinChat(otherUserId: String) {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) return
        
        isLoading.value = true
        // El ID del chat debe ser único para la combinación de estos dos usuarios.
        // Ordenamos los IDs para que el resultado sea siempre el mismo sin importar quién inició el chat.
        val users = listOf(currentUserId, otherUserId).sorted()
        currentChatId = "${users[0]}_${users[1]}"
        
        // Obtener el nombre del otro usuario (opcional, para la barra superior)
        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    otherUserName.value = doc.getString("name") ?: "Usuario Sordo"
                }
            }

        // Suscribirse a los mensajes en tiempo real
        db.collection("remote_chats")
            .document(currentChatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    errorMessage.value = "Error al sincronizar chat: ${e.message}"
                    Log.e("RemoteChat", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val msg = doc.toObject(RemoteMessage::class.java)
                        if (msg != null) {
                            messages.add(msg)
                        }
                    }
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentChatId.isEmpty() || currentUserId.isEmpty()) return
        
        val msg = RemoteMessage(
            senderId = currentUserId,
            senderName = currentUserName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        
        // Guardamos el mensaje en Firestore
        db.collection("remote_chats")
            .document(currentChatId)
            .collection("messages")
            .document(msg.id)
            .set(msg)
            .addOnFailureListener {
                errorMessage.value = "No se pudo enviar el mensaje."
            }
            
        // También podemos guardar metadatos del chat en el documento principal
        val chatMeta = hashMapOf(
            "lastMessage" to text,
            "lastUpdated" to System.currentTimeMillis(),
            "participants" to listOf(currentUserId, currentChatId.replace(currentUserId, "").replace("_", ""))
        )
        db.collection("remote_chats").document(currentChatId).set(chatMeta)
    }
}
