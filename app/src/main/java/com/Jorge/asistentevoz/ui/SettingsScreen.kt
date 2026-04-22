package com.Jorge.asistentevoz.ui

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.Jorge.asistentevoz.viewmodel.ChatViewModel
import com.Jorge.asistentevoz.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.Jorge.asistentevoz.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tts: TextToSpeech?,
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(viewModel.voiceSpeed.floatValue) }
    var pitch by remember { mutableFloatStateOf(viewModel.voicePitch.floatValue) }

    // Obtenemos un AuthViewModel solo para el manejo global de la imagen
    val authViewModel: AuthViewModel = viewModel()
    val photoUrl by authViewModel.profileImageUrl.collectAsState()

    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                // Usamos la nueva función reactiva global
                authViewModel.uploadProfileImage(uri)
                // Opcional: Podrías añadir un estado "isUploading" real en el ViewModel,
                // por ahora lo emulamos localmente con un pequeño delay o esperando a que photoUrl cambie.
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración y Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(24.dp)
        ) {
            // --- SECCIÓN DE PERFIL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Usamos nuestro nuevo componente reutilizable
                    ProfileAvatar(
                        imageUrl = photoUrl,
                        size = 100.dp,
                        onClick = {
                            if (!isUploading) galleryLauncher.launch("image/*")
                        }
                    )

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Ícono de cámara superpuesto
                    if (!isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Cambiar foto",
                                    modifier = Modifier.padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Personaliza el Asistente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ajusta cómo quieres que suene el teléfono cuando lee tus mensajes en voz alta.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // TARJETA DE VELOCIDAD
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Velocidad de Lectura", fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.1f", speed)}x", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        onValueChangeFinished = { viewModel.updateVoiceSettings(speed, pitch) },
                        valueRange = 0.5f..2.0f,
                        steps = 14
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TARJETA DE TONO
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tono de Voz (Pitch)", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when {
                                pitch < 0.8f -> "Grave"
                                pitch > 1.2f -> "Agudo"
                                else -> "Normal"
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = it },
                        onValueChangeFinished = { viewModel.updateVoiceSettings(speed, pitch) },
                        valueRange = 0.5f..2.0f,
                        steps = 14
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTONES DE PRUEBA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = {
                        speed = 1.0f
                        pitch = 1.0f
                        viewModel.updateVoiceSettings(1.0f, 1.0f)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar")
                }

                Button(
                    onClick = {
                        tts?.setSpeechRate(speed)
                        tts?.setPitch(pitch)
                        tts?.speak("Hola, así sonará mi voz.", TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar Voz")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE CERRAR SESIÓN ---
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error // Lo hace rojo para indicar "Peligro/Salir"
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}