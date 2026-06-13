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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.ui.detalle.DetalleReporteSheet
import com.example.bachewatch.ui.detalle.DetalleViewModel
import com.example.bachewatch.ui.theme.colorDeSeveridad
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberTileOverlayState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng

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
    detalleViewModel: DetalleViewModel = hiltViewModel(),
) {
    val reportes by viewModel.reportes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val modoHeatmap by viewModel.modoHeatmap.collectAsState()
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
    val reporteSeleccionado = reportes.firstOrNull { it.id == seleccionId }

    // Query on idle, not on every frame — no query storm while panning.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                viewModel.onViewportChanged(bounds.toGeoBounds())
            }
        }
    }

    LaunchedEffect(seleccionId, reporteSeleccionado) {
        when {
            seleccionId == null -> detalleViewModel.cerrar()
            reporteSeleccionado == null -> {
                detalleViewModel.cerrar()
                seleccionId = null
            }
            else -> detalleViewModel.mostrar(reporteSeleccionado)
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
                if (modoHeatmap) {
                    HeatmapReportes(reportes)
                } else {
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
                                    true
                                },
                            )
                        }
                    }
                }
            }

            FilterChip(
                selected = modoHeatmap,
                onClick = viewModel::toggleModoHeatmap,
                label = { Text("Zonas") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            // Subtle progress while the first snapshot resolves.
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (reporteSeleccionado != null) {
                DetalleReporteSheet(
                    viewModel = detalleViewModel,
                    onDismissRequest = {
                        seleccionId = null
                        detalleViewModel.cerrar()
                    },
                )
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

@Composable
private fun HeatmapReportes(reportes: List<Reporte>) {
    val weighted = remember(reportes) {
        reportes.map { reporte ->
            WeightedLatLng(
                LatLng(reporte.lat, reporte.lng),
                reporte.severidad?.peso ?: Severidad.PESO_SIN_SEVERIDAD,
            )
        }
    }
    val overlayState = rememberTileOverlayState()
    var provider by remember { mutableStateOf<HeatmapTileProvider?>(null) }
    var heatmapConDatos by remember { mutableStateOf(false) }
    val providerActual by rememberUpdatedState(provider)

    LaunchedEffect(weighted) {
        if (weighted.isEmpty()) {
            heatmapConDatos = false
            return@LaunchedEffect
        }
        val actual = providerActual
        if (actual == null) {
            provider = HeatmapTileProvider.Builder()
                .weightedData(weighted)
                .radius(36)
                .build()
        } else {
            actual.setWeightedData(weighted)
            overlayState.clearTileCache()
        }
        heatmapConDatos = true
    }

    if (weighted.isNotEmpty() && heatmapConDatos) {
        provider?.let {
            TileOverlay(
                tileProvider = it,
                state = overlayState,
            )
        }
    }
}

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
