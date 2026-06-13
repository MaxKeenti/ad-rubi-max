package com.example.bachewatch.ui.reportar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachewatch.data.location.LocationProvider
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportarUiState(
    // TODO(task 04): real Uri from TakePicture + FileProvider; until
    // then a simulated photo keeps the Day-1 flow demoable end-to-end.
    val fotoLista: Boolean = false,
    val fix: LocationFix? = null,
    val buscandoFix: Boolean = false,
    val fixError: String? = null,
    val severidad: Severidad? = null,
    val descripcion: String = "",
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val envioError: String? = null,
) {
    // TODO(task 04): also gate on fotoLista once the camera is real.
    val puedeEnviar: Boolean get() = fix != null && !enviando && !enviado
}

@HiltViewModel
class ReportarViewModel @Inject constructor(
    private val repository: ReporteRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportarUiState())
    val uiState: StateFlow<ReportarUiState> = _uiState

    init {
        // Warm-up trick (Q4): the fix request starts as soon as the
        // screen opens, hiding its latency behind the camera time.
        obtenerFix()
    }

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

    fun simularFoto() {
        _uiState.update { it.copy(fotoLista = true) }
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
        val fix = estado.fix ?: return
        _uiState.update { it.copy(enviando = true, envioError = null) }
        viewModelScope.launch {
            repository.crearReporte(
                fotoUri = Uri.EMPTY, // TODO(task 04): captured photo Uri
                fix = fix,
                severidad = estado.severidad,
                descripcion = estado.descripcion.ifBlank { null },
            )
                .onSuccess { _uiState.update { it.copy(enviando = false, enviado = true) } }
                .onFailure { e ->
                    // In-place retry (Q7): the composed report stays on screen.
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
