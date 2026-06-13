package com.example.bachewatch.ui.reportar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.ui.theme.SeveridadModerado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarScreen(
    onBack: () -> Unit,
    viewModel: ReportarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
            FotoCard(fotoLista = state.fotoLista, onTomarFoto = viewModel::simularFoto)

            FixCard(
                fix = state.fix,
                buscando = state.buscandoFix,
                error = state.fixError,
                onReintentar = viewModel::obtenerFix,
            )

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
private fun FotoCard(fotoLista: Boolean, onTomarFoto: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (fotoLista) Icons.Filled.CheckCircle else Icons.Filled.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            if (fotoLista) {
                Text("Foto lista ✓ (simulada)", style = MaterialTheme.typography.bodyMedium)
            } else {
                OutlinedButton(onClick = onTomarFoto) {
                    Text("Tomar foto (simulada — tarea 04)")
                }
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
