package com.Jorge.asistentevoz.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Conversation(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessage: String,
    val lastUpdated: Long
)

class ConversationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    val conversations = mutableStateListOf<Conversation>()
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")

    init {
        fetchConversations()
    }

    private fun fetchConversations() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        isLoading.value = true
        db.collection("remote_chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading.value = false
                    errorMessage.value = "Error al cargar conversaciones: ${e.message}"
                    Log.e("Conversations", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tempConversations = mutableListOf<Conversation>()
                    var pendingTasks = snapshot.documents.size

                    if (pendingTasks == 0) {
                        conversations.clear()
                        isLoading.value = false
                        return@addSnapshotListener
                    }

                    for (doc in snapshot.documents) {
                        @Suppress("UNCHECKED_CAST")
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastUpdated = doc.getLong("lastUpdated") ?: 0L
                        
                        val otherUserId = participants.find { it != currentUserId } ?: ""
                        
                        if (otherUserId.isNotEmpty()) {
                            // Fetch user name
                            db.collection("users").document(otherUserId).get()
                                .addOnSuccessListener { userDoc ->
                                    val otherUserName = userDoc.getString("name") ?: "Usuario Desconocido"
                                    tempConversations.add(
                                        Conversation(
                                            chatId = doc.id,
                                            otherUserId = otherUserId,
                                            otherUserName = otherUserName,
                                            lastMessage = lastMessage,
                                            lastUpdated = lastUpdated
                                        )
                                    )
                                    pendingTasks--
                                    if (pendingTasks == 0) {
                                        updateConversations(tempConversations)
                                    }
                                }
                                .addOnFailureListener {
                                    tempConversations.add(
                                        Conversation(
                                            chatId = doc.id,
                                            otherUserId = otherUserId,
                                            otherUserName = "Usuario Desconocido",
                                            lastMessage = lastMessage,
                                            lastUpdated = lastUpdated
                                        )
                                    )
                                    pendingTasks--
                                    if (pendingTasks == 0) {
                                        updateConversations(tempConversations)
                                    }
                                }
                        } else {
                            pendingTasks--
                            if (pendingTasks == 0) {
                                updateConversations(tempConversations)
                            }
                        }
                    }
                }
            }
    }
    
    private fun updateConversations(newList: List<Conversation>) {
        conversations.clear()
        conversations.addAll(newList.sortedByDescending { it.lastUpdated })
        isLoading.value = false
    }
}
