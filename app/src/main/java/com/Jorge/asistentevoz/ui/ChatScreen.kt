package com.Jorge.asistentevoz.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    // ESTADOS PARA EL BUSCADOR
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ESTADO PARA EL MENÚ DE 3 PUNTITOS
    var showMenu by remember { mutableStateOf(false) }

    val historialChat = viewModel.historialChat
    val listState = rememberLazyListState()

    // Manejo de errores y guardado en la UI
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage.value) {
        if (viewModel.errorMessage.value.isNotEmpty()) {
            snackbarHostState.showSnackbar(viewModel.errorMessage.value)
            viewModel.errorMessage.value = "" // limpiar después de mostrar
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
            Toast.makeText(context, "Necesitamos el micrófono para escucharte 🎤", Toast.LENGTH_LONG).show()
        }
    }

    if (mostrarDialogoGuardar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGuardar = false },
            title = { Text("Guardar Clase") },
            text = {
                Column {
                    Text("Introduce un nombre para identificar esta sesión:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nombreClaseTemp,
                        onValueChange = { nombreClaseTemp = it },
                        placeholder = { Text("Ej: Clase de Sistemas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.guardarClaseActual(nombreClaseTemp)
                        nombreClaseTemp = ""
                        mostrarDialogoGuardar = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGuardar = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = viewModel.nombreClaseActual.value, 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar Apuntes")
                    }

                    IconButton(onClick = {
                        if (historialChat.isNotEmpty()) {
                            mostrarDialogoGuardar = true
                        } else {
                            Toast.makeText(context, "No hay nada que guardar", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar Clase")
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Compartir apuntes") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (historialChat.isNotEmpty()) {
                                        val titulo = viewModel.nombreClaseActual.value
                                        val textoACompartir = "🎓 *Apuntes de ComunicaTech: $titulo*\n\n" +
                                                historialChat.joinToString(separator = "\n\n") { mensaje ->
                                                    if (mensaje.esMio) "📝 *Tú:* ${mensaje.texto}"
                                                    else "🎙️ *Voz:* ${mensaje.texto}"
                                                }

                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, textoACompartir)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Compartir apuntes vía...")
                                        context.startActivity(shareIntent)
                                    } else {
                                        Toast.makeText(context, "No hay nada para compartir", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Nueva conversación") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.iniciarNuevaClase()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent, // El degradado del Scaffold lo absorberá
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(bottomEnd = 24.dp, bottomStart = 24.dp))
                                   .clip(RoundedCornerShape(bottomEnd = 24.dp, bottomStart = 24.dp))
                                   .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            )
        }
    ) { paddingValues ->
        val fondoDegradado = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(fondoDegradado)
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar en apuntes y títulos...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            val clasesMostradas = if (searchQuery.isBlank()) {
                viewModel.clasesGuardadas
            } else {
                viewModel.clasesGuardadas.filter { clase ->
                    clase.nombre.contains(searchQuery, ignoreCase = true) ||
                            clase.mensajes.any { it.texto.contains(searchQuery, ignoreCase = true) }
                }
            }

            if (clasesMostradas.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clasesMostradas) { clase ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (viewModel.nombreClaseActual.value == clase.nombre) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                                viewModel.cargarClase(clase)
                                isSearchActive = false
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = clase.nombre,
                                    color = if (viewModel.nombreClaseActual.value == clase.nombre) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.eliminarClase(clase) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Borrar", tint = if (viewModel.nombreClaseActual.value == clase.nombre) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else if (searchQuery.isNotBlank()) {
                Text(
                    text = "No se encontraron clases con '$searchQuery'",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // --- AQUI INYECTAMOS LA FASE 2: EL ESTADO VACÍO ILUSTRADO ---
                if (historialChat.isEmpty()) {
                    item {
                        EstadoVacioChat()
                    }
                }
                items(historialChat) { mensaje -> BurbujaChat(mensaje = mensaje) }
            }

            if (isRecording) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escuchando...", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            // === ÁREA DE INPUT FLOTANTE PREMIUM ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Caja de Texto Redondeada
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe un mensaje...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            maxLines = 4,
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        tts?.speak(inputText, TextToSpeech.QUEUE_FLUSH, null, null)
                                        viewModel.agregarMensaje(MensajeChat(texto = inputText, esMio = true))
                                        inputText = ""
                                    }) { 
                                        Icon(
                                            Icons.Default.Send, 
                                            contentDescription = "Enviar", 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Botón de Micrófono Flotante
                    FloatingActionButton(
                        onClick = {
                            if (!isRecording) {
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
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
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.padding(bottom = 2.dp) // Para alinear con la caja de texto
                    ) { 
                        Icon(Icons.Default.Mic, contentDescription = "Micrófono", tint = MaterialTheme.colorScheme.onPrimary) 
                    }
                }
            }
        }
    }
}

// ==========================================
// --- COMPONENTES VISUALES PREMIUM ---
// ==========================================

// FASE 2: Pantalla de Estado Vacío
@Composable
fun EstadoVacioChat() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, bottom = 32.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(120.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.padding(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tu lienzo en blanco",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Presiona el micrófono para empezar a transcribir o escribe tu primer mensaje.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// FASE 1: Burbujas de Chat Premium
@Composable
fun BurbujaChat(mensaje: MensajeChat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = if (mensaje.esMio) Arrangement.End else Arrangement.Start
    ) {
        val horaFormateada = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()) }

        Column(
            horizontalAlignment = if (mensaje.esMio) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentWidth(if (mensaje.esMio) Alignment.End else Alignment.Start)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = if (mensaje.esMio) 4.dp else 16.dp,
                            bottomStart = if (mensaje.esMio) 16.dp else 4.dp
                        )
                    )
                    .background(
                        color = if (mensaje.esMio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = if (mensaje.esMio) 4.dp else 16.dp,
                            bottomStart = if (mensaje.esMio) 16.dp else 4.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = mensaje.texto,
                        color = if (mensaje.esMio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = horaFormateada,
                        color = if (mensaje.esMio) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}