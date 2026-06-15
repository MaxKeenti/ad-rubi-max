package com.example.bachewatch.ui.recientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import com.example.bachewatch.data.util.tiempoRelativo
import com.example.bachewatch.ui.detalle.DetalleReporteSheet
import com.example.bachewatch.ui.detalle.DetalleViewModel
import com.example.bachewatch.ui.theme.colorDeSeveridad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecientesScreen(
    onBack: () -> Unit,
    viewModel: RecientesViewModel = hiltViewModel(),
    detalleViewModel: DetalleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var seleccionId by remember { mutableStateOf<String?>(null) }
    val reporteSeleccionado = state.reportes.firstOrNull { it.id == seleccionId }

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
                title = { Text("Recientes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.cargando -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> {
                val error = state.error.orEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            state.reportes.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Aún no hay reportes")
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.reportes, key = { it.id }) { reporte ->
                    ReporteRow(
                        reporte = reporte,
                        onClick = { seleccionId = reporte.id },
                    )
                }
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

@Composable
private fun ReporteRow(
    reporte: Reporte,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = reporte.fotoUrl,
                contentDescription = "Foto del reporte",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TipoBadge(reporte.tipo)
                    SeveridadBadge(reporte.severidad)
                    Text(
                        tiempoRelativo(reporte.serverWrittenAt),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    reporte.descripcion ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                Text(
                    "✓ ${reporte.confirmCount} confirmaciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TipoBadge(tipo: TipoIncidencia) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            tipo.etiqueta,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SeveridadBadge(severidad: Severidad?) {
    val color = colorDeSeveridad(severidad)
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            severidad?.valor ?: "sin severidad",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
