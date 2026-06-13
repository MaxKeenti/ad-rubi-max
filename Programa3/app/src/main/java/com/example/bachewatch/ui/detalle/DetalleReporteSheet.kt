package com.example.bachewatch.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.bachewatch.data.auth.SesionAnonima
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.data.util.tiempoRelativo
import com.example.bachewatch.ui.theme.colorDeSeveridad
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleUiState(
    val reporte: Reporte? = null,
    val uid: String? = null,
    val cargandoConfirmacion: Boolean = false,
    val yaConfirmo: Boolean = false,
    val confirmando: Boolean = false,
    val eliminando: Boolean = false,
    val eliminado: Boolean = false,
    val optimisticConfirmCount: Long? = null,
    val error: String? = null,
) {
    val confirmCountMostrado: Long
        get() = optimisticConfirmCount ?: reporte?.confirmCount ?: 0

    val puedeConfirmar: Boolean
        get() = reporte != null &&
            !cargandoConfirmacion &&
            !yaConfirmo &&
            !confirmando &&
            !eliminando

    val puedeEliminar: Boolean
        get() = reporte?.let {
            uid != null &&
                it.createdBy == uid &&
                dentroDeVentanaEliminar(it.serverWrittenAt) &&
                !eliminando
        } == true
}

@HiltViewModel
class DetalleViewModel @Inject constructor(
    private val repository: ReporteRepository,
    private val sesion: SesionAnonima,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleUiState(uid = sesion.uid.value))
    val uiState: StateFlow<DetalleUiState> = _uiState

    init {
        viewModelScope.launch {
            sesion.uid.collect { uid ->
                _uiState.update { it.copy(uid = uid) }
            }
        }
    }

    fun mostrar(reporte: Reporte) {
        val estado = _uiState.value
        val mismoReporte = estado.reporte?.id == reporte.id
        val optimistic = estado.optimisticConfirmCount
            ?.takeUnless { reporte.confirmCount >= it }

        if (mismoReporte) {
            _uiState.update {
                it.copy(
                    reporte = reporte,
                    optimisticConfirmCount = optimistic,
                    eliminado = false,
                )
            }
            return
        }

        _uiState.value = DetalleUiState(
            reporte = reporte,
            uid = estado.uid,
            cargandoConfirmacion = true,
        )
        viewModelScope.launch {
            val confirmado = repository.yaConfirmo(reporte.id)
            _uiState.update {
                if (it.reporte?.id == reporte.id) {
                    it.copy(
                        cargandoConfirmacion = false,
                        yaConfirmo = confirmado,
                    )
                } else {
                    it
                }
            }
        }
    }

    fun cerrar() {
        _uiState.value = DetalleUiState(uid = _uiState.value.uid)
    }

    fun confirmar() {
        val reporte = _uiState.value.reporte ?: return
        if (!_uiState.value.puedeConfirmar) return

        _uiState.update {
            it.copy(
                confirmando = true,
                optimisticConfirmCount = reporte.confirmCount + 1,
                error = null,
            )
        }
        viewModelScope.launch {
            repository.confirmar(reporte.id)
                .onSuccess {
                    _uiState.update { estado ->
                        if (estado.reporte?.id == reporte.id) {
                            estado.copy(confirmando = false, yaConfirmo = true)
                        } else {
                            estado
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { estado ->
                        if (estado.reporte?.id == reporte.id) {
                            estado.copy(
                                confirmando = false,
                                optimisticConfirmCount = null,
                                error = e.message ?: "No se pudo confirmar",
                            )
                        } else {
                            estado
                        }
                    }
                }
        }
    }

    fun eliminar() {
        val reporte = _uiState.value.reporte ?: return
        if (!_uiState.value.puedeEliminar) return

        _uiState.update { it.copy(eliminando = true, error = null) }
        viewModelScope.launch {
            repository.eliminar(reporte.id)
                .onSuccess {
                    _uiState.update { estado ->
                        if (estado.reporte?.id == reporte.id) {
                            estado.copy(eliminando = false, eliminado = true)
                        } else {
                            estado
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { estado ->
                        if (estado.reporte?.id == reporte.id) {
                            estado.copy(
                                eliminando = false,
                                error = e.message ?: "No se pudo eliminar",
                            )
                        } else {
                            estado
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleReporteSheet(
    onDismissRequest: () -> Unit,
    viewModel: DetalleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val reporte = state.reporte ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmarEliminacion by remember(reporte.id) { mutableStateOf(false) }

    LaunchedEffect(state.eliminado) {
        if (state.eliminado) onDismissRequest()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = reporte.fotoUrl,
                contentDescription = "Foto del bache",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeveridadBadge(reporte.severidad)
                Text(
                    tiempoRelativo(reporte.serverWrittenAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                reporte.descripcion ?: "Sin descripción",
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.confirmCountMostrado} confirmaciones",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = viewModel::confirmar,
                    enabled = state.puedeConfirmar,
                ) {
                    when {
                        state.confirmando -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmando...")
                        }
                        state.yaConfirmo -> {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text("Confirmado ✓")
                        }
                        else -> Text("Confirmar")
                    }
                }
            }

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.puedeEliminar) {
                OutlinedButton(
                    onClick = { confirmarEliminacion = true },
                    enabled = !state.eliminando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(if (state.eliminando) "Eliminando..." else "Eliminar")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("Eliminar reporte") },
            text = {
                Text("Se eliminará tu reporte. Las fotos borrosas se reportan de nuevo, no se editan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                        viewModel.eliminar()
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminacion = false }) {
                    Text("Cancelar")
                }
            },
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

private fun dentroDeVentanaEliminar(serverWrittenAt: Timestamp?): Boolean {
    val escritoMs = serverWrittenAt?.toDate()?.time ?: return false
    val edadMs = System.currentTimeMillis() - escritoMs
    return edadMs < 24L * 60L * 60L * 1_000L
}
