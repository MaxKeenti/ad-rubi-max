package com.example.bachewatch.ui.recientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RecientesViewModel @Inject constructor(
    repository: ReporteRepository,
) : ViewModel() {

    val reportes: StateFlow<List<Reporte>> = repository.recientes(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
