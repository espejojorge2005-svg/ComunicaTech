package com.Jorge.asistentevoz.ui

import android.os.Build.VERSION.SDK_INT
import android.speech.tts.TextToSpeech
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.Jorge.asistentevoz.R
import com.Jorge.asistentevoz.viewmodel.SenasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignLanguageScreen(
    tts: TextToSpeech?,
    onBackClick: () -> Unit,
    viewModel: SenasViewModel = viewModel()
) {
    val selectedTabIndex by viewModel.selectedTabIndex

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.shadow(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CenterAlignedTopAppBar(
                        title = { 
                            Text("Modo Sordo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary) }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Segmented Control Style Tab
                    Surface(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp).fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if(selectedTabIndex==0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.cambiarPestana(0) },
                                contentAlignment = Alignment.Center
                            ) { Text("Abecedario", color = if(selectedTabIndex==0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
                            
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if(selectedTabIndex==1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.cambiarPestana(1) },
                                contentAlignment = Alignment.Center
                            ) { Text("Frases Animadas", color = if(selectedTabIndex==1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8)) // Fondo moderno y limpio
        ) {
            Crossfade(targetState = selectedTabIndex, label = "Tab Transition") { tab ->
                when (tab) {
                    0 -> VistaDiccionarioEstablePremium(tts = tts, viewModel = viewModel)
                    1 -> VistaFrasesAnimadasPremium(tts = tts, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VistaDiccionarioEstablePremium(tts: TextToSpeech?, viewModel: SenasViewModel) {
    val textoEntrada by viewModel.textoAbecedario
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky Header area for Input
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = textoEntrada,
                    onValueChange = { viewModel.actualizarTextoAbecedario(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("¿Qué deseas traducir a señas?") },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        if (textoEntrada.isNotEmpty()) {
                            IconButton(onClick = { viewModel.actualizarTextoAbecedario("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f),
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { if (textoEntrada.isNotEmpty()) tts?.speak(textoEntrada, TextToSpeech.QUEUE_FLUSH, null, null) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reproducir Audio", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Área visual
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (textoEntrada.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SignLanguage, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha=0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("El lienzo está vacío", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Escribe arriba para visualizar el abecedario", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val caracteres = textoEntrada.uppercase().filter { it.isLetter() || it.isWhitespace() }
                Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize().padding(horizontal = 16.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        caracteres.forEach { char ->
                            if (char.isWhitespace()) Spacer(modifier = Modifier.width(32.dp))
                            else TarjetaSenaPremium(letra = char)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaSenaPremium(letra: Char) {
    val letraMinuscula = letra.lowercaseChar()
    val imageRes = when (letraMinuscula) {
        'a' -> R.drawable.sena_a; 'b' -> R.drawable.sena_b; 'c' -> R.drawable.sena_c; 'd' -> R.drawable.sena_d
        'e' -> R.drawable.sena_e; 'f' -> R.drawable.sena_f; 'g' -> R.drawable.sena_g; 'h' -> R.drawable.sena_h
        'i' -> R.drawable.sena_i; 'j' -> R.drawable.sena_j; 'k' -> R.drawable.sena_k; 'l' -> R.drawable.sena_l
        'm' -> R.drawable.sena_m; 'n' -> R.drawable.sena_n; 'o' -> R.drawable.sena_o; 'p' -> R.drawable.sena_p
        'q' -> R.drawable.sena_q; 'r' -> R.drawable.sena_r; 's' -> R.drawable.sena_s; 't' -> R.drawable.sena_t
        'u' -> R.drawable.sena_u; 'v' -> R.drawable.sena_v; 'w' -> R.drawable.sena_w; 'x' -> R.drawable.sena_x
        'y' -> R.drawable.sena_y; 'z' -> R.drawable.sena_z
        else -> null
    }

    Surface(
        modifier = Modifier.width(100.dp).height(130.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = Modifier.padding(8.dp).fillMaxSize())
                } else {
                    Icon(Icons.Default.ImageNotSupported, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = letra.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun VistaFrasesAnimadasPremium(tts: TextToSpeech?, viewModel: SenasViewModel) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory()) }
            .build()
    }

    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (viewModel.errorMessage.value.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Ocurrió un problema de red", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.descargarGifsDesdeFirebase() }) { Text("Reintentar") }
            }
        }
    } else {
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = if (searchQuery.isEmpty()) viewModel.listaSenas else viewModel.listaSenas.filter { it.displayNombre.contains(searchQuery, true) }

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar frase...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { if(searchQuery.isNotEmpty()) IconButton(onClick = {searchQuery=""}){Icon(Icons.Default.Clear, null)} },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontró '$searchQuery'", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { sena ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth().clickable { tts?.speak(sena.displayNombre, TextToSpeech.QUEUE_FLUSH, null, null) }
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFE0E0E0))) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(sena.url_gif).crossfade(true).build(),
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sena.displayNombre.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2C3E50)
                                    )
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}