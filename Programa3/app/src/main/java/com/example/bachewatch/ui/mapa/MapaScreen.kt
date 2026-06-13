package com.example.bachewatch.ui.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.ui.theme.colorDeSeveridad
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

private val CDMX_CENTRO = LatLng(19.4326, -99.1332)

/**
 * Home (grilling Q13): live severity-colored markers driven by
 * `observarViewport`, re-queried as the camera settles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onReportar: () -> Unit,
    onRecientes: () -> Unit,
    viewModel: MapaViewModel = hiltViewModel(),
) {
    val reportes by viewModel.reportes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val context = LocalContext.current

    // The map never prompts (task 05) — it only reflects an existing grant.
    val permisoUbicacion = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CDMX_CENTRO, 12f)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var seleccionId by remember { mutableStateOf<String?>(null) }

    // Query on idle, not on every frame — no query storm while panning.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.onViewportChanged(bounds.toGeoBounds())
            }
        }
    }

    // Detail sheet lands in task 09; until then a snackbar with the id.
    LaunchedEffect(seleccionId) {
        seleccionId?.let {
            snackbarHostState.showSnackbar("Reporte $it")
            seleccionId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BacheWatch") },
                actions = {
                    IconButton(onClick = onRecientes) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Recientes",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportar,
                icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                text = { Text("Reportar") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = permisoUbicacion),
            ) {
                // Cache one BitmapDescriptor per severity; built lazily after the
                // map is ready (BitmapDescriptorFactory needs Maps initialized).
                val iconos = remember { mutableMapOf<Severidad?, BitmapDescriptor>() }
                reportes.forEach { reporte ->
                    key(reporte.id) {
                        Marker(
                            state = rememberMarkerState(
                                key = reporte.id,
                                position = LatLng(reporte.lat, reporte.lng),
                            ),
                            icon = iconos.getOrPut(reporte.severidad) {
                                iconoMarcador(colorDeSeveridad(reporte.severidad).toArgb())
                            },
                            title = reporte.descripcion ?: "Bache",
                            snippet = "✓ ${reporte.confirmCount}",
                            tag = reporte.id,
                            onClick = {
                                seleccionId = reporte.id
                                false // keep default behavior (info window + recenter)
                            },
                        )
                    }
                }
            }

            // Subtle progress while the first snapshot resolves.
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/** Keep the GeoBounds ↔ LatLngBounds conversion in the UI layer (ADR-0002). */
private fun LatLngBounds.toGeoBounds() = GeoBounds(
    swLat = southwest.latitude,
    swLng = southwest.longitude,
    neLat = northeast.latitude,
    neLng = northeast.longitude,
)

/** A filled dot in the severity color (matches the Recientes chips) with a white ring. */
private fun iconoMarcador(colorArgb: Int): BitmapDescriptor {
    val tamano = 48
    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radio = tamano / 2f
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    pincel.color = AndroidColor.WHITE
    canvas.drawCircle(radio, radio, radio, pincel)
    pincel.color = colorArgb
    canvas.drawCircle(radio, radio, radio * 0.74f, pincel)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
