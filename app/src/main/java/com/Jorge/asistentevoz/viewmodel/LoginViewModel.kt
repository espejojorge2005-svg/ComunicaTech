package com.Jorge.asistentevoz.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val isLoginMode = mutableStateOf(true)

    fun toggleMode() {
        isLoginMode.value = !isLoginMode.value
        errorMessage.value = "" // Limpiar errores al cambiar
    }

    fun authenticate(
        nombre: String,
        correo: String,
        contrasena: String,
        onSuccess: (String) -> Unit
    ) {
        val emailTrims = correo.trim()
        
        // 1. Validaciones básicas antes de tocar Firebase
        if (emailTrims.isBlank() || contrasena.isBlank()) {
            errorMessage.value = "Por favor, llena todos los campos obligatorios."
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrims).matches()) {
            errorMessage.value = "El formato del correo no es válido."
            return
        }

        if (contrasena.length < 6) {
            errorMessage.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        if (!isLoginMode.value && nombre.isBlank()) {
            errorMessage.value = "Por favor, ingresa tu nombre."
            return
        }

        // Todo bien, procedemos a Firebase
        isLoading.value = true
        errorMessage.value = ""

        if (isLoginMode.value) {
            // LOGIN
            auth.signInWithEmailAndPassword(emailTrims, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userName = user?.displayName ?: "Usuario"
                        
                        // Guardar/Actualizar en Firestore
                        if (user != null) {
                            val userMap = hashMapOf(
                                "uid" to user.uid,
                                "name" to userName,
                                "email" to user.email
                            )
                            db.collection("users").document(user.uid).set(userMap)
                        }

                        isLoading.value = false
                        onSuccess(userName)
                    } else {
                        isLoading.value = false
                        errorMessage.value = "Correo o contraseña incorrectos."
                    }
                }
        } else {
            // REGISTRO
            auth.createUserWithEmailAndPassword(emailTrims, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre.trim())
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            // Guardar en Firestore
                            if (user != null) {
                                val userMap = hashMapOf(
                                    "uid" to user.uid,
                                    "name" to nombre.trim(),
                                    "email" to user.email
                                )
                                db.collection("users").document(user.uid).set(userMap)
                            }
                            
                            isLoading.value = false
                            onSuccess(nombre.trim())
                        }
                    } else {
                        isLoading.value = false
                        errorMessage.value = "Error al registrar. Puede que el correo ya esté en uso."
                    }
                }
        }
    }
}
