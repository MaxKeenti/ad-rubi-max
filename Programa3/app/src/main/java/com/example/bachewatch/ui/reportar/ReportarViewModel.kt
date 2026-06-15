package com.example.bachewatch.ui.reportar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachewatch.data.location.LocationProvider
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import com.example.bachewatch.data.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportarUiState(
    /** Content Uri of the captured photo (FileProvider, cacheDir). */
    val fotoUri: Uri? = null,
    val fix: LocationFix? = null,
    val buscandoFix: Boolean = false,
    val fixError: String? = null,
    val tipo: TipoIncidencia = TipoIncidencia.BACHE,
    val severidad: Severidad? = null,
    val descripcion: String = "",
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val envioError: String? = null,
) {
    val fotoLista: Boolean get() = fotoUri != null

    /** Foto + fix are both mandatory (Q3); tipo/severidad/descripción never gate. */
    val puedeEnviar: Boolean get() = fotoUri != null && fix != null && !enviando && !enviado
}

@HiltViewModel
class ReportarViewModel @Inject constructor(
    private val repository: ReporteRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportarUiState())
    val uiState: StateFlow<ReportarUiState> = _uiState

    /**
     * Warm-up trick (Q4): the screen calls this *as the camera launches*,
     * so the fix resolves during the 5–15 s the user spends framing the
     * shot. Also the "Reintentar" handler on the fix card.
     */
    fun obtenerFix() {
        _uiState.update { it.copy(buscandoFix = true, fixError = null) }
        viewModelScope.launch {
            locationProvider.fixActual()
                .onSuccess { fix ->
                    _uiState.update { it.copy(fix = fix, buscandoFix = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            buscandoFix = false,
                            fixError = e.message ?: "No se pudo obtener la ubicación",
                        )
                    }
                }
        }
    }

    fun onFotoCapturada(uri: Uri) {
        _uiState.update { it.copy(fotoUri = uri) }
    }

    fun onTipo(tipo: TipoIncidencia) {
        _uiState.update { it.copy(tipo = tipo) }
    }

    /** Re-tap deselects: null severidad must stay reachable (Q6). */
    fun onSeveridad(severidad: Severidad) {
        _uiState.update {
            it.copy(severidad = if (it.severidad == severidad) null else severidad)
        }
    }

    fun onDescripcion(texto: String) {
        _uiState.update { it.copy(descripcion = texto.take(200)) }
    }

    fun enviar() {
        val estado = _uiState.value
        val fotoUri = estado.fotoUri ?: return
        val fix = estado.fix ?: return
        _uiState.update { it.copy(enviando = true, envioError = null) }
        viewModelScope.launch {
            repository.crearReporte(
                fotoUri = fotoUri,
                fix = fix,
                tipo = estado.tipo,
                severidad = estado.severidad,
                descripcion = estado.descripcion.ifBlank { null },
            )
                .onSuccess { _uiState.update { it.copy(enviando = false, enviado = true) } }
                .onFailure { e ->
                    // In-place retry (Q7): the composed report stays on screen,
                    // nothing survives process death by design.
                    _uiState.update {
                        it.copy(
                            enviando = false,
                            envioError = e.message ?: "No se pudo enviar; reintenta",
                        )
                    }
                }
        }
    }
}
