package com.example.bachewatch.ui.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberTileOverlayState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

private val CDMX_CENTRO = LatLng(19.4326, -99.1332)
private const val OSM_ZOOM = 13
private const val OSM_MIN_ZOOM = 4
private const val OSM_MAX_ZOOM = 20
private const val OSM_LOCATION_ZOOM = 17

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
    val ubicacion by viewModel.ubicacion.collectAsState()
    val ubicacionError by viewModel.ubicacionError.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var centerOnLocationRequest by remember { mutableStateOf(0) }

    // The map never prompts (task 05) — it only reflects an existing grant.
    var permisoUbicacion by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcherUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permisoUbicacion = granted
        if (granted) {
            centerOnLocationRequest++
            viewModel.obtenerUbicacion()
        }
    }
    val solicitarOCentrarUbicacion = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permisoUbicacion = true
            centerOnLocationRequest++
            viewModel.obtenerUbicacion()
        } else {
            launcherUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(permisoUbicacion) {
        if (permisoUbicacion && ubicacion == null) {
            viewModel.obtenerUbicacion()
        }
    }

    LaunchedEffect(ubicacionError) {
        ubicacionError?.let { snackbarHostState.showSnackbar(it) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CDMX_CENTRO, 12f)
    }
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
            OsmTileMap(
                reportes = reportes,
                modoHeatmap = modoHeatmap,
                ubicacion = ubicacion,
                centerOnLocationRequest = centerOnLocationRequest,
                onReporteClick = { seleccionId = it.id },
                onViewportChanged = viewModel::onViewportChanged,
            )

            FilterChip(
                selected = modoHeatmap,
                onClick = viewModel::toggleModoHeatmap,
                label = { Text("Zonas") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                IconButton(onClick = solicitarOCentrarUbicacion) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Mi ubicacion",
                    )
                }
            }

            // Subtle progress while the first snapshot resolves.
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (modoHeatmap && !cargando && reportes.isEmpty()) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    Text(
                        text = "No hay reportes en esta zona",
                        modifier = Modifier.padding(16.dp),
                    )
                }
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

@Composable
private fun OsmTileMap(
    reportes: List<Reporte>,
    modoHeatmap: Boolean,
    ubicacion: com.example.bachewatch.data.model.LocationFix?,
    centerOnLocationRequest: Int,
    onReporteClick: (Reporte) -> Unit,
    onViewportChanged: (GeoBounds) -> Unit,
) {
    val density = LocalDensity.current
    val tileSize = 256.dp
    var zoom by remember { mutableStateOf(OSM_ZOOM) }
    var center by remember { mutableStateOf(CDMX_CENTRO.toTilePoint(OSM_ZOOM)) }
    val zoomBy: (Int) -> Unit = { delta ->
        val nextZoom = (zoom + delta).coerceIn(OSM_MIN_ZOOM, OSM_MAX_ZOOM)
        if (nextZoom != zoom) {
            val latLng = center.toLatLng(zoom)
            zoom = nextZoom
            center = latLng.toTilePoint(nextZoom)
        }
    }

    LaunchedEffect(ubicacion, centerOnLocationRequest) {
        ubicacion?.let {
            zoom = OSM_LOCATION_ZOOM
            center = LatLng(it.lat, it.lng).toTilePoint(zoom)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEAE4))
            .pointerInput(zoom) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val tileSizePx = with(density) { tileSize.toPx() }
                    val nextZoom = (zoom + when {
                        gestureZoom > 1.08f -> 1
                        gestureZoom < 0.92f -> -1
                        else -> 0
                    }).coerceIn(OSM_MIN_ZOOM, OSM_MAX_ZOOM)
                    if (nextZoom != zoom) {
                        val latLng = center.toLatLng(zoom)
                        zoom = nextZoom
                        center = latLng.toTilePoint(nextZoom)
                    } else {
                        center = center.copy(
                            x = center.x - pan.x / tileSizePx,
                            y = center.y - pan.y / tileSizePx,
                        )
                    }
                }
            },
    ) {
        val tileSizePx = with(density) { tileSize.toPx() }
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val tilesX = ceil(widthPx / tileSizePx).toInt() + 4
        val tilesY = ceil(heightPx / tileSizePx).toInt() + 4
        val baseX = floor(center.x).toInt()
        val baseY = floor(center.y).toInt()

        LaunchedEffect(center, zoom, widthPx, heightPx) {
            onViewportChanged(center.toGeoBounds(zoom, widthPx, heightPx, tileSizePx))
        }

        for (dx in -tilesX / 2..tilesX / 2) {
            for (dy in -tilesY / 2..tilesY / 2) {
                val x = baseX + dx
                val y = baseY + dy
                val left = widthPx / 2f + ((x - center.x) * tileSizePx).toFloat()
                val top = heightPx / 2f + ((y - center.y) * tileSizePx).toFloat()

                AsyncImage(
                    model = "https://a.basemaps.cartocdn.com/light_all/$zoom/$x/$y.png",
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(tileSize)
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) },
                )
            }
        }

        reportes.forEach { reporte ->
            key(reporte.id) {
                val punto = LatLng(reporte.lat, reporte.lng).toTilePoint(zoom)
                val x = widthPx / 2f + ((punto.x - center.x) * tileSizePx).toFloat()
                val y = heightPx / 2f + ((punto.y - center.y) * tileSizePx).toFloat()
                val size = if (modoHeatmap) {
                    ((reporte.severidad?.peso ?: Severidad.PESO_SIN_SEVERIDAD) * 18f).dp
                } else {
                    18.dp
                }
                val color = colorDeSeveridad(reporte.severidad)

                Box(
                    modifier = Modifier
                        .offset {
                            val markerPx = with(density) { size.toPx() }
                            IntOffset(
                                (x - markerPx / 2f).roundToInt(),
                                (y - markerPx / 2f).roundToInt(),
                            )
                        }
                        .size(size)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (modoHeatmap) 0.42f else 0.92f))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onReporteClick(reporte) },
                )
            }
        }

        ubicacion?.let { fix ->
            val punto = LatLng(fix.lat, fix.lng).toTilePoint(zoom)
            val x = widthPx / 2f + ((punto.x - center.x) * tileSizePx).toFloat()
            val y = heightPx / 2f + ((punto.y - center.y) * tileSizePx).toFloat()
            Box(
                modifier = Modifier
                    .offset {
                        val markerPx = with(density) { 20.dp.toPx() }
                        IntOffset(
                            (x - markerPx / 2f).roundToInt(),
                            (y - markerPx / 2f).roundToInt(),
                        )
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A73E8))
                    .border(3.dp, Color.White, CircleShape),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp),
        ) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                IconButton(
                    onClick = { zoomBy(1) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Acercar")
                }
            }
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                IconButton(
                    onClick = { zoomBy(-1) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Alejar")
                }
            }
        }
    }
}

private data class TilePoint(val x: Double, val y: Double)

private fun LatLng.toTilePoint(zoom: Int): TilePoint {
    val scale = 2.0.pow(zoom)
    val x = (longitude + 180.0) / 360.0 * scale
    val latRad = latitude * PI / 180.0
    val y = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * scale
    return TilePoint(x, y)
}

private fun TilePoint.toLatLng(zoom: Int): LatLng {
    val scale = 2.0.pow(zoom)
    val lng = x / scale * 360.0 - 180.0
    val latRad = kotlin.math.atan(sinh(PI * (1.0 - 2.0 * y / scale)))
    val lat = latRad * 180.0 / PI
    return LatLng(lat, lng)
}

private fun TilePoint.toGeoBounds(
    zoom: Int,
    widthPx: Float,
    heightPx: Float,
    tileSizePx: Float,
): GeoBounds {
    val halfTilesX = widthPx / 2f / tileSizePx
    val halfTilesY = heightPx / 2f / tileSizePx
    val sw = TilePoint(x - halfTilesX, y + halfTilesY).toLatLng(zoom)
    val ne = TilePoint(x + halfTilesX, y - halfTilesY).toLatLng(zoom)
    return GeoBounds(
        swLat = sw.latitude,
        swLng = sw.longitude,
        neLat = ne.latitude,
        neLng = ne.longitude,
    )
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
