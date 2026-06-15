package com.example.bachewatch.ui.mapa

import android.net.Uri
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `viewport change re queries the repository and ignores duplicate bounds`() = runTest {
        val repository = RecordingReporteRepository()
        val viewModel = MapaViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.reportes.collect {}
        }
        advanceUntilIdle()

        val newBounds = GeoBounds(swLat = 19.30, swLng = -99.20, neLat = 19.45, neLng = -99.00)
        viewModel.onViewportChanged(newBounds)
        viewModel.onViewportChanged(newBounds)
        advanceUntilIdle()

        assertEquals(listOf(GeoBounds.CDMX, newBounds), repository.viewportCalls)
        assertFalse(viewModel.cargando.value)
    }

    @Test
    fun `heatmap toggle preserves the current reports`() = runTest {
        val reports = listOf(Reporte(id = "r1", lat = 19.40, lng = -99.10, severidad = Severidad.SEVERO))
        val repository = RecordingReporteRepository(reports)
        val viewModel = MapaViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.reportes.collect {}
        }
        advanceUntilIdle()

        viewModel.toggleModoHeatmap()

        assertTrue(viewModel.modoHeatmap.value)
        assertEquals(reports, viewModel.reportes.value)
        assertEquals(listOf(GeoBounds.CDMX), repository.viewportCalls)
    }

    private class RecordingReporteRepository(
        private val reports: List<Reporte> = emptyList(),
    ) : ReporteRepository {

        val viewportCalls = mutableListOf<GeoBounds>()

        override suspend fun crearReporte(
            fotoUri: Uri,
            fix: LocationFix,
            tipo: TipoIncidencia,
            severidad: Severidad?,
            descripcion: String?,
        ): Result<String> = throw AssertionError("crearReporte is not used by MapaViewModel")

        override fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>> {
            viewportCalls += bounds
            return flowOf(reports)
        }

        override fun recientes(limit: Int): Flow<List<Reporte>> =
            throw AssertionError("recientes is not used by MapaViewModel")

        override suspend fun confirmar(reporteId: String): Result<Unit> =
            throw AssertionError("confirmar is not used by MapaViewModel")

        override suspend fun yaConfirmo(reporteId: String): Boolean =
            throw AssertionError("yaConfirmo is not used by MapaViewModel")

        override suspend fun eliminar(reporteId: String): Result<Unit> =
            throw AssertionError("eliminar is not used by MapaViewModel")
    }
}
