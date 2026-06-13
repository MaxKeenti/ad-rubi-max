package com.example.bachewatch.ui.mapa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Home (grilling Q13). The GoogleMap composable replaces the
 * placeholder card in task 07; the ViewModel already runs the real
 * viewport pipeline against fakes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onReportar: () -> Unit,
    onRecientes: () -> Unit,
    viewModel: MapaViewModel = hiltViewModel(),
) {
    val reportes by viewModel.reportes.collectAsState()

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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Card(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Mapa de Google — llega con la tarea 07",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${reportes.size} reportes en el viewport (datos de prueba)",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
