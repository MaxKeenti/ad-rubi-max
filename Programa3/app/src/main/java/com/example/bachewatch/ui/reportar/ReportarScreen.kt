package com.example.bachewatch.ui.reportar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.ui.theme.SeveridadModerado
import java.io.File

private val PERMISOS_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarScreen(
    onBack: () -> Unit,
    viewModel: ReportarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Only the report flow asks for location (task 05); the read screens never do.
    var permisoFino by remember { mutableStateOf(tienePermisoFino(context)) }
    var fotoPendiente by remember { mutableStateOf<Uri?>(null) }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { resultado ->
        // Coarse-only is treated as denial for reporting (fine-or-nothing, Q5).
        permisoFino = resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (permisoFino && state.fotoLista && state.fix == null) viewModel.obtenerFix()
    }

    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { exito ->
        // TakePicture returns false on cancel — leave the screen usable, no error.
        if (exito) fotoPendiente?.let { viewModel.onFotoCapturada(it) }
    }

    fun lanzarCamara() {
        val uri = crearUriFoto(context)
        fotoPendiente = uri
        // Warm-up trick (Q4): start the fix as the camera opens.
        if (permisoFino) viewModel.obtenerFix()
        camaraLauncher.launch(uri)
    }

    // Resolve permission on entry so the warm-up fix can run when the camera opens.
    LaunchedEffect(Unit) {
        if (!permisoFino) permisoLauncher.launch(PERMISOS_UBICACION)
    }

    LaunchedEffect(state.enviado) {
        if (state.enviado) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar bache") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FotoCard(fotoUri = state.fotoUri, onTomarFoto = ::lanzarCamara)

            if (permisoFino) {
                FixCard(
                    fix = state.fix,
                    buscando = state.buscandoFix,
                    error = state.fixError,
                    onReintentar = viewModel::obtenerFix,
                )
            } else {
                PermisoUbicacionCard(
                    onConceder = { permisoLauncher.launch(PERMISOS_UBICACION) },
                    onAjustes = { abrirAjustes(context) },
                )
            }

            Text("Severidad (opcional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Severidad.entries.forEach { severidad ->
                    FilterChip(
                        selected = state.severidad == severidad,
                        onClick = { viewModel.onSeveridad(severidad) },
                        label = {
                            Text(severidad.valor.replaceFirstChar { it.uppercase() })
                        },
                    )
                }
            }

            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcion,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción (opcional)") },
                supportingText = { Text("${state.descripcion.length}/200") },
                minLines = 2,
            )

            state.envioError?.let { mensaje ->
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::enviar,
                enabled = state.puedeEnviar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.enviando -> "Enviando…"
                        state.envioError != null -> "Reintentar"
                        else -> "Enviar reporte"
                    }
                )
            }
        }
    }
}

@Composable
private fun FotoCard(fotoUri: Uri?, onTomarFoto: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (fotoUri != null) {
                AsyncImage(
                    model = fotoUri,
                    contentDescription = "Foto del bache",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                OutlinedButton(onClick = onTomarFoto) { Text("Volver a tomar") }
            } else {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Button(onClick = onTomarFoto) { Text("Tomar foto") }
            }
        }
    }
}

@Composable
private fun FixCard(
    fix: LocationFix?,
    buscando: Boolean,
    error: String?,
    onReintentar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                buscando -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Obteniendo ubicación…")
                }
                fix != null -> {
                    val precisa = fix.accuracyMeters <= LocationFix.UMBRAL_PRECISION_M
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = if (precisa) MaterialTheme.colorScheme.primary else SeveridadModerado,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ubicación capturada (±${fix.accuracyMeters.toInt()} m)")
                        if (!precisa) {
                            // Soft gate (Q5): warn, never block.
                            Text(
                                "Precisión baja — puedes guardar de todos modos o reintentar",
                                style = MaterialTheme.typography.bodySmall,
                                color = SeveridadModerado,
                            )
                        }
                    }
                    TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
                else -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sin ubicación", color = MaterialTheme.colorScheme.error)
                        error?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun PermisoUbicacionCard(onConceder: () -> Unit, onAjustes: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Se necesita ubicación precisa para reportar")
                Text(
                    "El resto de la app funciona sin permiso; solo reportar lo requiere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onConceder) { Text("Conceder") }
                    TextButton(onClick = onAjustes) { Text("Abrir ajustes") }
                }
            }
        }
    }
}

private fun tienePermisoFino(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/** A fresh cacheDir Uri the system camera can write to (no storage perms). */
private fun crearUriFoto(context: Context): Uri {
    val archivo = File.createTempFile("bache_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

private fun abrirAjustes(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
