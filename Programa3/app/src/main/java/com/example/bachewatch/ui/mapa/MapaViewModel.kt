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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MapaViewModel @Inject constructor(
    repository: ReporteRepository,
) : ViewModel() {

    private val viewport = MutableStateFlow(GeoBounds.CDMX)

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando

    private val _modoHeatmap = MutableStateFlow(false)
    val modoHeatmap: StateFlow<Boolean> = _modoHeatmap

    // flatMapLatest cancels the previous viewport's listeners on pan —
    // the intended lifecycle (the screen wires camera-idle to this).
    @OptIn(ExperimentalCoroutinesApi::class)
    val reportes: StateFlow<List<Reporte>> = viewport
        .flatMapLatest(repository::observarViewport)
        .onEach { _cargando.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onViewportChanged(bounds: GeoBounds) {
        if (bounds == viewport.value) return
        _cargando.value = true
        viewport.value = bounds
    }

    fun toggleModoHeatmap() {
        _modoHeatmap.value = !_modoHeatmap.value
    }
}
