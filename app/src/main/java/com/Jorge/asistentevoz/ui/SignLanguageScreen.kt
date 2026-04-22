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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    val fondoDegradado = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                   .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                   .background(MaterialTheme.colorScheme.primary)
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SignLanguage, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Modo Sordo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 4.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { viewModel.cambiarPestana(0) },
                        text = { Text("Abecedario", fontWeight = if(selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { viewModel.cambiarPestana(1) },
                        text = { Text("Frases Animadas", fontWeight = if(selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(fondoDegradado)
        ) {
            Crossfade(targetState = selectedTabIndex, label = "Tab Transition") { tab ->
                when (tab) {
                    0 -> VistaDiccionarioEstable(tts = tts, viewModel = viewModel)
                    1 -> VistaFrasesAnimadas(tts = tts, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VistaDiccionarioEstable(tts: TextToSpeech?, viewModel: SenasViewModel) {
    val textoEntrada by viewModel.textoAbecedario
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            OutlinedTextField(
                value = textoEntrada,
                onValueChange = { viewModel.actualizarTextoAbecedario(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe aquí para traducir a señas...") },
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    if (textoEntrada.isNotEmpty()) {
                        IconButton(onClick = { viewModel.actualizarTextoAbecedario("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (textoEntrada.isNotEmpty()) {
                    tts?.speak(textoEntrada, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Hablar")
            Spacer(modifier = Modifier.width(12.dp))
            Text("Reproducir en Voz Alta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            if (textoEntrada.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SignLanguage, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu lienzo visual está vacío",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Empieza a escribir arriba para ver la magia.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                val caracteres = textoEntrada.uppercase().filter { it.isLetter() || it.isWhitespace() }
                Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        caracteres.forEach { char ->
                            if (char.isWhitespace()) {
                                Spacer(modifier = Modifier.width(48.dp))
                            } else {
                                TarjetaSena(letra = char)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaSena(letra: Char) {
    val letraMinuscula = letra.lowercaseChar()
    val imageRes = when (letraMinuscula) {
        'a' -> R.drawable.sena_a
        'b' -> R.drawable.sena_b
        'c' -> R.drawable.sena_c
        'd' -> R.drawable.sena_d
        'e' -> R.drawable.sena_e
        'f' -> R.drawable.sena_f
        'g' -> R.drawable.sena_g
        'h' -> R.drawable.sena_h
        'i' -> R.drawable.sena_i
        'j' -> R.drawable.sena_j
        'k' -> R.drawable.sena_k
        'l' -> R.drawable.sena_l
        'm' -> R.drawable.sena_m
        'n' -> R.drawable.sena_n
        'o' -> R.drawable.sena_o
        'p' -> R.drawable.sena_p
        'q' -> R.drawable.sena_q
        'r' -> R.drawable.sena_r
        's' -> R.drawable.sena_s
        't' -> R.drawable.sena_t
        'u' -> R.drawable.sena_u
        'v' -> R.drawable.sena_v
        'w' -> R.drawable.sena_w
        'x' -> R.drawable.sena_x
        'y' -> R.drawable.sena_y
        'z' -> R.drawable.sena_z
        else -> null
    }

    Surface(
        modifier = Modifier
            .width(110.dp)
            .height(140.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Piso 1 (Imagen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Seña de la letra $letra",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Imagen no disponible",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                }
            }

            // Divisor sutil
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            // Piso 2 (Texto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letra.uppercase(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun VistaFrasesAnimadas(tts: TextToSpeech?, viewModel: SenasViewModel) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) { add(ImageDecoderDecoder.Factory()) } 
                else { add(GifDecoder.Factory()) }
            }
            .build()
    }

    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Conectando con la nube...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    } else if (viewModel.errorMessage.value.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(100.dp)) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = viewModel.errorMessage.value, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.descargarGifsDesdeFirebase() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Intentar Nuevamente", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    } else {
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = if (searchQuery.isEmpty()) {
            viewModel.listaSenas
        } else {
            viewModel.listaSenas.filter { it.displayNombre.contains(searchQuery, ignoreCase = true) }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar frase animada...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Limpiar") }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "No hay resultados para '$searchQuery'", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList) { sena ->
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                tts?.speak(sena.displayNombre, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = sena.displayNombre.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(sena.url_gif)
                                        .crossfade(true)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = sena.displayNombre,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}