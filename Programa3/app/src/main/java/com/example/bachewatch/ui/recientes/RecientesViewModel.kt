package com.example.bachewatch.ui.recientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class RecientesUiState(
    val reportes: List<Reporte> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class RecientesViewModel @Inject constructor(
    repository: ReporteRepository,
) : ViewModel() {

    val uiState: StateFlow<RecientesUiState> = repository.recientes(50)
        .map { RecientesUiState(reportes = it, cargando = false) }
        .catch {
            emit(
                RecientesUiState(
                    cargando = false,
                    error = "No se pudieron cargar los reportes",
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecientesUiState())
}
