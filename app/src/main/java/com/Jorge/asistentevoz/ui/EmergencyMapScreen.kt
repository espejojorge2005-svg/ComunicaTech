package com.Jorge.asistentevoz.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

enum class EmergencyPointType {
    HOSPITAL, POLICE, FIREFIGHTER
}

data class EmergencyPoint(
    val id: String,
    val name: String,
    val type: EmergencyPointType,
    val location: LatLng
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyMapScreen(
    modifier: Modifier = Modifier,
    emergencyPoints: List<EmergencyPoint> = getEmergencyPoints(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            
            if (fineLocationGranted || coarseLocationGranted) {
                hasLocationPermission = true
                showPermissionRationale = false
            } else {
                showPermissionRationale = true
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permiso de Ubicación Necesario") },
            text = { Text("La ubicación es vital para mostrarte las rutas más rápidas hacia puntos de ayuda y mejorar tu seguridad en situaciones críticas.") },
            confirmButton = {
                TextButton(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Conceder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Centro inicial: Trujillo, Perú
    val trujilloLocation = LatLng(-8.1119, -79.0287)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(trujilloLocation, 13f)
    }

    // Estado de filtros
    var showHospitals by remember { mutableStateOf(true) }
    var showPolice by remember { mutableStateOf(true) }
    var showFireStations by remember { mutableStateOf(true) }

    val filteredPoints = emergencyPoints.filter { point ->
        when (point.type) {
            EmergencyPointType.HOSPITAL -> showHospitals
            EmergencyPointType.POLICE -> showPolice
            EmergencyPointType.FIREFIGHTER -> showFireStations
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Emergencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
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
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            val mapProperties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
            )
            val mapUiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                compassEnabled = true,
                zoomControlsEnabled = true
            )

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                filteredPoints.forEach { point ->
                    val color = when (point.type) {
                        EmergencyPointType.HOSPITAL -> BitmapDescriptorFactory.HUE_RED
                        EmergencyPointType.POLICE -> BitmapDescriptorFactory.HUE_BLUE
                        EmergencyPointType.FIREFIGHTER -> BitmapDescriptorFactory.HUE_ORANGE
                    }

                    Marker(
                        state = MarkerState(position = point.location),
                        title = point.name,
                        icon = BitmapDescriptorFactory.defaultMarker(color)
                    )
                }
            }

            // Fila de Filtros Rápidos (Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showHospitals,
                    onClick = { showHospitals = !showHospitals },
                    label = { Text("Hospitales") }
                )
                FilterChip(
                    selected = showPolice,
                    onClick = { showPolice = !showPolice },
                    label = { Text("Comisarías") }
                )
                FilterChip(
                    selected = showFireStations,
                    onClick = { showFireStations = !showFireStations },
                    label = { Text("Bomberos") }
                )
            }
        }
    }
}

// Datos reales de Trujillo, Perú
fun getEmergencyPoints(): List<EmergencyPoint> {
    return listOf(
        // HOSPITALES
        EmergencyPoint("h1", "Hospital Regional Docente de Trujillo", EmergencyPointType.HOSPITAL, LatLng(-8.1065, -79.0384)),
        EmergencyPoint("h2", "Hospital Belén de Trujillo", EmergencyPointType.HOSPITAL, LatLng(-8.1147, -79.0298)),
        EmergencyPoint("h3", "Hospital Víctor Lazarte Echegaray - EsSalud", EmergencyPointType.HOSPITAL, LatLng(-8.1167, -79.0156)),
        EmergencyPoint("h4", "Hospital Alta Complejidad Virgen de la Puerta", EmergencyPointType.HOSPITAL, LatLng(-8.0776, -79.0435)),
        
        // COMISARÍAS
        EmergencyPoint("p1", "Comisaría Ayacucho", EmergencyPointType.POLICE, LatLng(-8.1105, -79.0267)),
        EmergencyPoint("p2", "Comisaría El Alambre", EmergencyPointType.POLICE, LatLng(-8.1126, -79.0416)),
        EmergencyPoint("p3", "Comisaría La Noria", EmergencyPointType.POLICE, LatLng(-8.1133, -79.0152)),
        EmergencyPoint("p4", "Comisaría Buenos Aires", EmergencyPointType.POLICE, LatLng(-8.1293, -79.0431)),
        
        // BOMBEROS
        EmergencyPoint("f1", "Cía. de Bomberos Salvadora Trujillo N° 26", EmergencyPointType.FIREFIGHTER, LatLng(-8.1137, -79.0274)),
        EmergencyPoint("f2", "Cía. de Bomberos Washington State N° 177", EmergencyPointType.FIREFIGHTER, LatLng(-8.1004, -79.0163)),
        EmergencyPoint("f3", "Cía. de Bomberos Víctor Larco N° 224", EmergencyPointType.FIREFIGHTER, LatLng(-8.1278, -79.0486))
    )
}
