package com.Jorge.asistentevoz.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@android.annotation.SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    tts: TextToSpeech?,
    onBackClick: () -> Unit,
    onContactScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Escáner Inteligente", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                if (isScanning) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val options = BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                    .build()
                                val scanner = BarcodeScanning.getClient(options)
                                val executor = Executors.newSingleThreadExecutor()

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && isScanning) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    barcode.rawValue?.let { valorDetectado ->
                                                        if (valorDetectado.startsWith("comunicatech://contact/")) {
                                                            val contactId = valorDetectado.removePrefix("comunicatech://contact/")
                                                            isScanning = false
                                                            tts?.speak("Contacto detectado, abriendo chat", TextToSpeech.QUEUE_FLUSH, null, null)
                                                            onContactScanned(contactId)
                                                        } else {
                                                            scannedText = valorDetectado
                                                            isScanning = false
                                                            // Reproducir voz
                                                            tts?.speak("Código escaneado: $valorDetectado", TextToSpeech.QUEUE_FLUSH, null, null)
                                                        }
                                                        break
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    Log.e("ScannerScreen", "Use case binding failed", exc)
                                }

                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay / Marco de Escaneo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val rectWidth = canvasWidth * 0.7f
                        val rectHeight = rectWidth
                        
                        // Oscurecer todo excepto el cuadro central
                        drawRect(Color.Black.copy(alpha = 0.5f))
                        
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset((canvasWidth - rectWidth) / 2, (canvasHeight - rectHeight) / 2),
                            size = Size(rectWidth, rectHeight),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                        )
                        
                        // Dibujar los bordes del escáner
                        drawRoundRect(
                            color = Color(0xFF0085C7), // Celeste Corporativo
                            topLeft = Offset((canvasWidth - rectWidth) / 2, (canvasHeight - rectHeight) / 2),
                            size = Size(rectWidth, rectHeight),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Apunta a un Código QR para leerlo en voz alta",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Pantalla de Resultado
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.wrapContentHeight()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("¡Código leído con éxito!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = scannedText,
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                tts?.speak(scannedText, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Repetir en Voz Alta", fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                scannedText = ""
                                isScanning = true
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Escanear Otro Código", fontSize = 16.sp)
                        }
                    }
                }
            } else {
                // Pantalla de Permiso Denegado
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📷", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Permiso de Cámara Requerido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Necesitamos acceso a la cámara para poder escanear los códigos QR del entorno.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Conceder Permiso")
                    }
                }
            }
        }
    }
}
