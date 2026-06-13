package com.example.bachewatch.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MapaViewModel @Inject constructor(
    repository: ReporteRepository,
) : ViewModel() {

    private val viewport = MutableStateFlow(GeoBounds.CDMX)

    // flatMapLatest cancels the previous viewport's listeners on pan —
    // the intended lifecycle (task 07 wires camera-idle to this).
    @OptIn(ExperimentalCoroutinesApi::class)
    val reportes: StateFlow<List<Reporte>> = viewport
        .flatMapLatest(repository::observarViewport)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onViewportChanged(bounds: GeoBounds) {
        viewport.value = bounds
    }
}
