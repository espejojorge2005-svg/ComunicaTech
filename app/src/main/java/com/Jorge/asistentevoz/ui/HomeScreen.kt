package com.Jorge.asistentevoz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nombreUsuario: String,
    photoUrl: String?,
    onModoOyenteClick: () -> Unit,
    onModoSordoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewsClick: () -> Unit,
    onScannerClick: () -> Unit,
    onProfileClick: () -> Unit,
    onConversationsClick: () -> Unit,
    onEmergencyMapClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Fondo degradado elegante para la cabecera
    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ComunicaTech", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.QrCode, contentDescription = "Mi Perfil QR")
                    }
                    IconButton(onClick = onSettingsClick) {
                        if (!photoUrl.isNullOrEmpty()) {
                            val fallbackPainter = rememberVectorPainter(image = Icons.Default.AccountCircle)
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Configuración y Perfil",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                fallback = fallbackPainter,
                                error = fallbackPainter
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Configuración y Perfil",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScannerClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear QR") },
                text = { Text("Escanear", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .verticalScroll(scrollState)
        ) {
            // --- SECCIÓN DE BIENVENIDA ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Column {
                        Text(
                            text = "¡Hola, $nombreUsuario! 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¿Qué te gustaría hacer hoy?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN DE TARJETAS DE NAVEGACIÓN ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp) // Mayor respiro entre tarjetas
            ) {
                // Tarjeta 1: Modo Oyente (Chatbot)
                TarjetaMenu(
                    titulo = "Modo Oyente",
                    subtitulo = "Conversación fluida por voz y texto.",
                    icono = Icons.Default.Mic,
                    onClick = onModoOyenteClick
                )

                // Tarjeta 2: Modo Sordo (Señas)
                TarjetaMenu(
                    titulo = "Modo Sordo",
                    subtitulo = "Traductor visual a lengua de señas.",
                    icono = Icons.Default.PanTool,
                    onClick = onModoSordoClick
                )

                // Tarjeta 3: Noticias Tech
                TarjetaMenu(
                    titulo = "Noticias Tech",
                    subtitulo = "Entérate de lo último en tecnología.",
                    icono = Icons.Default.Public,
                    onClick = onNewsClick
                )

                // Tarjeta 4: Mis Chats
                TarjetaMenu(
                    titulo = "Mis Chats",
                    subtitulo = "Revisa tus conversaciones recientes.",
                    icono = Icons.Default.Chat,
                    onClick = onConversationsClick
                )

                // Tarjeta 5: Mapa de Ayuda
                TarjetaMenu(
                    titulo = "Mapa de Ayuda",
                    subtitulo = "Encuentra hospitales y comisarías.",
                    icono = Icons.Default.Map,
                    onClick = onEmergencyMapClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// COMPONENTE: TARJETA DE MENÚ PREMIUM (M3)
// ==========================================
@Composable
fun TarjetaMenu(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            // Círculo con el ícono y fondo contrastante
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}