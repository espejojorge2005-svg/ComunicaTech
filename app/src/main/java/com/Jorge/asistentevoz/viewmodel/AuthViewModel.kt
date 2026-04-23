package com.Jorge.asistentevoz.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val name: String, val photoUrl: String?) : AuthState()
    data class SuccessRegistrationNeedsVerification(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class ErrorVerificationNeeded(val message: String, val email: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    init {
        _profileImageUrl.value = auth.currentUser?.photoUrl?.toString()
    }

    // Compatibilidad temporal
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: return@launch
                val storageRef = storage.reference.child("profile_pics/${user.uid}.jpg")

                // a) Subir a Firebase Storage y b) Obtener downloadUrl
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                // c) Actualizar el perfil de Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUrl)
                    .build()
                user.updateProfile(profileUpdates).await()

                // d) Sincronizar con Firestore para consistencia total
                db.collection("users").document(user.uid)
                    .update("photoUrl", downloadUrl.toString()).await()

                // e) Actualizar el estado local observable
                _profileImageUrl.value = downloadUrl.toString()

            } catch (e: Exception) {
                // Propagar el error a la UI para que pueda mostrarlo
                _authState.value = AuthState.Error(
                    e.localizedMessage ?: "Error al subir la imagen de perfil."
                )
            }
        }
    }

    // ------------------------------------------
    // LÓGICA DE LOGIN CON VERIFICACIÓN
    // ------------------------------------------

    fun loginUser(email: String, password: String) {
        val emailTrim = email.trim()
        
        if (emailTrim.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Por favor, llena todos los campos.")
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(emailTrim, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    
                    if (user?.isEmailVerified == true) {
                        val userName = user.displayName ?: "Usuario"
                        val photoUrl = user.photoUrl?.toString()
                        _authState.value = AuthState.Success(userName, photoUrl)
                    } else {
                        // NO ESTÁ VERIFICADO
                        auth.signOut()
                        _authState.value = AuthState.ErrorVerificationNeeded(
                            message = "Por favor, verifica tu correo electrónico para poder ingresar. Revisa tu bandeja de entrada o spam.",
                            email = emailTrim
                        )
                    }
                } else {
                    _authState.value = AuthState.Error("Correo o contraseña incorrectos.")
                }
            }
    }

    fun resendVerificationEmail(email: String, password: String) {
        val emailTrim = email.trim()
        _authState.value = AuthState.Loading
        
        auth.signInWithEmailAndPassword(emailTrim, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && !user.isEmailVerified) {
                        user.sendEmailVerification().addOnCompleteListener { emailTask ->
                            auth.signOut()
                            if (emailTask.isSuccessful) {
                                _authState.value = AuthState.Error("Correo de verificación reenviado. Revisa tu bandeja de entrada o spam.")
                            } else {
                                _authState.value = AuthState.Error("No se pudo reenviar el correo. Intenta más tarde.")
                            }
                        }
                    } else {
                        auth.signOut()
                        _authState.value = AuthState.Error("El correo ya está verificado. Por favor inicia sesión normalmente.")
                    }
                } else {
                    _authState.value = AuthState.Error("Credenciales incorrectas para reenviar verificación.")
                }
            }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // ------------------------------------------
    // LÓGICA DE REGISTRO CON VERIFICACIÓN
    // ------------------------------------------

    fun registerUser(
        nombre: String,
        correo: String,
        contrasena: String,
        imageUri: Uri?
    ) {
        val emailTrims = correo.trim()
        val nameTrim = nombre.trim()

        if (nameTrim.isBlank() || emailTrims.isBlank() || contrasena.isBlank()) {
            _authState.value = AuthState.Error("Por favor, llena todos los campos obligatorios.")
            return
        }

        if (contrasena.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                // 1. Crear el usuario
                val authResult = auth.createUserWithEmailAndPassword(emailTrims, contrasena).await()
                val user = authResult.user ?: throw Exception("No se pudo obtener el usuario.")

                // 2. Subir la imagen a Storage y obtener la downloadUrl
                var photoUrl: String? = null
                if (imageUri != null) {
                    try {
                        val storageRef = storage.reference.child("profile_pics/${user.uid}.jpg")
                        storageRef.putFile(imageUri).await()
                        photoUrl = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        // Si falla la imagen, continuamos sin foto para no bloquear el registro
                    }
                }

                // 3. Llamar a updateProfile con la nueva URL de la foto
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nameTrim)
                
                if (photoUrl != null) {
                    profileUpdates.setPhotoUri(Uri.parse(photoUrl))
                }
                
                // 4. ESPERAR a que updateProfile sea exitoso
                user.updateProfile(profileUpdates.build()).await()

                // Actualizar en Firestore de forma segura
                val userMap = hashMapOf(
                    "uid" to user.uid,
                    "name" to nameTrim,
                    "email" to emailTrims,
                    "photoUrl" to (photoUrl ?: "")
                )
                db.collection("users").document(user.uid).set(userMap).await()

                // 5. Solo después del éxito del paso 4, llamar a sendEmailVerification() y auth.signOut()
                user.sendEmailVerification().await()
                auth.signOut()

                _authState.value = AuthState.SuccessRegistrationNeedsVerification(emailTrims)

            } catch (e: Exception) {
                // En caso de cualquier error durante el flujo
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al registrar la cuenta.")
            }
        }
    }
}
