package com.Jorge.asistentevoz.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Jorge.asistentevoz.viewmodel.ChatViewModel
import com.Jorge.asistentevoz.viewmodel.MensajeChat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val isRecording by viewModel.isRecording
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    var mostrarDialogoGuardar by remember { mutableStateOf(false) }
    var nombreClaseTemp by remember { mutableStateOf("") }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val historialChat = viewModel.historialChat
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage.value) {
        if (viewModel.errorMessage.value.isNotEmpty()) {
            snackbarHostState.showSnackbar(viewModel.errorMessage.value)
            viewModel.errorMessage.value = ""
        }
    }

    LaunchedEffect(viewModel.isSaving.value) {
        if (viewModel.isSaving.value) {
            snackbarHostState.showSnackbar("Guardando en la nube...")
        }
    }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) { tts?.language = Locale("es", "PE") }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(historialChat.size) {
        if (historialChat.isNotEmpty()) {
            listState.animateScrollToItem(historialChat.size - 1)
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.toggleRecording()
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoReconocido = results?.firstOrNull() ?: ""
            if (textoReconocido.isNotEmpty()) {
                viewModel.agregarMensaje(MensajeChat(texto = textoReconocido, esMio = false))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleRecording()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            }
            try { speechRecognizerLauncher.launch(intent) } catch (e: Exception) { viewModel.toggleRecording() }
        } else {
            Toast.makeText(context, "Se requiere micrófono 🎤", Toast.LENGTH_LONG).show()
        }
    }

    if (mostrarDialogoGuardar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGuardar = false },
            title = { Text("Guardar Conversación", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nombreClaseTemp,
                    onValueChange = { nombreClaseTemp = it },
                    placeholder = { Text("Ej: Reunión de diseño") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.guardarClaseActual(nombreClaseTemp)
                    nombreClaseTemp = ""
                    mostrarDialogoGuardar = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGuardar = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.shadow(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Modo Oyente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AnimatedVisibility(visible = viewModel.nombreClaseActual.value.isNotEmpty()) {
                                Text(
                                    text = viewModel.nombreClaseActual.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) { 
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary) 
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary) }

                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Guardar chat") },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (historialChat.isNotEmpty()) mostrarDialogoGuardar = true
                                    else Toast.makeText(context, "El chat está vacío", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir transcripción") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (historialChat.isNotEmpty()) {
                                        val textoACompartir = historialChat.joinToString(separator = "\n") { 
                                            if (it.esMio) "Yo: ${it.texto}" else "Asistente: ${it.texto}" 
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, textoACompartir)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Compartir con"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpiar chat") },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; viewModel.iniciarNuevaClase() }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8)) // Fondo moderno y limpio
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Barra de búsqueda animada
                AnimatedVisibility(visible = isSearchActive) {
                    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Buscar mensajes...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                            )
                        )
                    }
                }

                // Lista de chats históricos
                val clasesMostradas = viewModel.clasesGuardadas.filter { 
                    it.nombre.contains(searchQuery, true) || it.mensajes.any { m -> m.texto.contains(searchQuery, true) } 
                }
                AnimatedVisibility(visible = clasesMostradas.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clasesMostradas) { clase ->
                            val isSelected = viewModel.nombreClaseActual.value == clase.nombre
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if(!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                modifier = Modifier.clickable { viewModel.cargarClase(clase); isSearchActive = false }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = clase.nombre,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Borrar",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp).clickable { viewModel.eliminarClase(clase) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Área Principal de Chat
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    if (historialChat.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "La conversación está vacía",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Presiona el micrófono para transcribir",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(historialChat) { mensaje -> BurbujaChatPremium(mensaje) }
                    }
                }

                // Animación de "Escuchando"
                AnimatedVisibility(visible = isRecording, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f, targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse)
                                )
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escuchando el entorno...", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Área de Input Flotante Moderno
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe para leer en voz alta...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Crossfade(targetState = inputText.isNotEmpty()) { isTexting ->
                            if (isTexting) {
                                FloatingActionButton(
                                    onClick = {
                                        tts?.speak(inputText, TextToSpeech.QUEUE_FLUSH, null, null)
                                        viewModel.agregarMensaje(MensajeChat(texto = inputText, esMio = true))
                                        inputText = ""
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = CircleShape
                                ) { Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White) }
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        if (!isRecording) {
                                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                viewModel.toggleRecording()
                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
                                                }
                                                try { speechRecognizerLauncher.launch(intent) } catch (e: Exception) { viewModel.toggleRecording() }
                                            } else {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = CircleShape
                                ) { Icon(Icons.Default.Mic, contentDescription = "Micrófono", tint = if(isRecording) Color.White else MaterialTheme.colorScheme.onSecondaryContainer) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BurbujaChatPremium(mensaje: MensajeChat) {
    val esMio = mensaje.esMio
    val bgColor = if (esMio) MaterialTheme.colorScheme.primary else Color.White
    val textColor = if (esMio) Color.White else Color(0xFF2C3E50)
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        if (!esMio) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.padding(6.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .wrapContentWidth(if (esMio) Alignment.End else Alignment.Start)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomEnd = if (esMio) 4.dp else 20.dp,
                    bottomStart = if (esMio) 20.dp else 4.dp
                ),
                color = bgColor,
                shadowElevation = if(esMio) 2.dp else 1.dp
            ) {
                Text(
                    text = mensaje.texto,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        if (esMio) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(6.dp), tint = Color.White)
            }
        }
    }
}