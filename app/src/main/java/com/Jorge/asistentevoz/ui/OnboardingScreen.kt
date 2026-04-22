package com.Jorge.asistentevoz.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishTutorial: () -> Unit // Función que llamaremos al terminar
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val paginas = listOf(
        OnboardingPageInfo(
            titulo = "Bienvenido a ComunicaTech",
            descripcion = "Una herramienta diseñada para romper las barreras de comunicación. Habla, escucha y exprésate sin límites.",
            icono = Icons.Default.Favorite
        ),
        OnboardingPageInfo(
            titulo = "Modo Oyente Inteligente",
            descripcion = "Convierte voz a texto en tiempo real. Guarda tus conversaciones, busca en tus apuntes y compártelos fácilmente.",
            icono = Icons.Default.Hearing
        ),
        OnboardingPageInfo(
            titulo = "Modo Sordo (Señas)",
            descripcion = "Escribe para que tu teléfono hable por ti, y visualiza el texto traducido al lenguaje de señas al instante.",
            icono = Icons.Default.PanTool
        )
    )

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicadores de puntitos (Dots)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Botón para avanzar o finalizar
                if (pagerState.currentPage == 2) {
                    Button(onClick = onFinishTutorial) {
                        Text("Empezar")
                    }
                } else {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("Siguiente")
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            OnboardingPage(info = paginas[page])
        }
    }
}

@Composable
fun OnboardingPage(info: OnboardingPageInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(160.dp)
        ) {
            Icon(
                imageVector = info.icono,
                contentDescription = null,
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxSize(),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = info.titulo,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = info.descripcion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Clase de datos simple para organizar la info de cada página
data class OnboardingPageInfo(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector
)