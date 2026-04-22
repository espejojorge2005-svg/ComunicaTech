package com.Jorge.asistentevoz.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val userId = currentUser?.uid ?: ""

    // Estado del nombre (reactivo para que se actualice en pantalla después de guardar)
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Usuario") }

    // Estado del diálogo de edición
    var showEditDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var saveSuccess by remember { mutableStateOf(false) }

    val qrContent = "comunicatech://contact/$userId"

    // Diálogo para editar el nombre
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showEditDialog = false
                    saveError = ""
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Editar Nombre", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Este nombre será visible para las personas con las que chatees.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it; saveError = "" },
                        label = { Text("Tu nombre") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        isError = saveError.isNotEmpty(),
                        supportingText = if (saveError.isNotEmpty()) {
                            { Text(saveError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editNameInput.trim()
                        if (trimmedName.isBlank()) {
                            saveError = "El nombre no puede estar vacío."
                            return@Button
                        }
                        isSaving = true

                        // 1. Actualizar Firebase Auth
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()

                        currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                // 2. Actualizar Firestore
                                val userMap = hashMapOf(
                                    "uid" to userId,
                                    "name" to trimmedName,
                                    "email" to (currentUser.email ?: "")
                                )
                                db.collection("users").document(userId).set(userMap)
                                    .addOnSuccessListener {
                                        displayName = trimmedName
                                        isSaving = false
                                        showEditDialog = false
                                        saveSuccess = true
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        saveError = "Error al guardar: ${e.message}"
                                    }
                            } else {
                                isSaving = false
                                saveError = "Error al actualizar el perfil."
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false; saveError = "" },
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Snackbar de éxito
            if (saveSuccess) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        "¡Nombre actualizado correctamente!",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Nombre con botón de editar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Hola, $displayName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        editNameInput = displayName
                        saveError = ""
                        saveSuccess = false
                        showEditDialog = true
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar nombre",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Para conectar con otra persona en tiempo real, muéstrale este Código QR y dile que lo escanee desde su aplicación ComunicaTech.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.size(280.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (userId.isNotEmpty()) {
                        val bitmap = generateQrCode(qrContent)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Código QR de Mi Perfil",
                                modifier = Modifier.size(240.dp)
                            )
                        } else {
                            Text("Error al generar QR", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text("No has iniciado sesión", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de editar nombre (secundario, debajo del QR)
            OutlinedButton(
                onClick = {
                    editNameInput = displayName
                    saveError = ""
                    saveSuccess = false
                    showEditDialog = true
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar mi nombre", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Código Único y Seguro",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Función auxiliar para generar el Bitmap del QR usando ZXing
fun generateQrCode(content: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
