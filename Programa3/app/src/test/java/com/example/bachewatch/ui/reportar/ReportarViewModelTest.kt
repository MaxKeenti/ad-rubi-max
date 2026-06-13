package com.example.bachewatch.ui.reportar

import android.net.Uri
import com.example.bachewatch.data.location.LocationError
import com.example.bachewatch.data.location.LocationProvider
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.test.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ReportarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `obtenerFix moves from loading to success`() = runTest {
        val locationProvider = ControlledLocationProvider()
        val viewModel = ReportarViewModel(RecordingReporteRepository(), locationProvider)
        val fix = LocationFix(lat = 19.40, lng = -99.10, accuracyMeters = 9.0)

        viewModel.obtenerFix()
        runCurrent()
        assertTrue(viewModel.uiState.value.buscandoFix)

        locationProvider.complete(Result.success(fix))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.buscandoFix)
        assertEquals(fix, viewModel.uiState.value.fix)
        assertNull(viewModel.uiState.value.fixError)
    }

    @Test
    fun `obtenerFix moves from loading to timeout error`() = runTest {
        val locationProvider = ControlledLocationProvider()
        val viewModel = ReportarViewModel(RecordingReporteRepository(), locationProvider)

        viewModel.obtenerFix()
        runCurrent()
        assertTrue(viewModel.uiState.value.buscandoFix)

        locationProvider.complete(Result.failure(LocationError.FixTimeout))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.buscandoFix)
        assertNull(viewModel.uiState.value.fix)
        assertEquals("No se pudo obtener la ubicacion; reintenta", normalized(viewModel.uiState.value.fixError))
    }

    @Test
    fun `severity can be deselected and description is truncated at two hundred characters`() {
        val viewModel = ReportarViewModel(RecordingReporteRepository(), ControlledLocationProvider())

        viewModel.onSeveridad(Severidad.SEVERO)
        assertEquals(Severidad.SEVERO, viewModel.uiState.value.severidad)

        viewModel.onSeveridad(Severidad.SEVERO)
        assertNull(viewModel.uiState.value.severidad)

        viewModel.onDescripcion("a".repeat(250))
        assertEquals(200, viewModel.uiState.value.descripcion.length)
    }

    @Test
    fun `submit failure keeps composed state and exposes retry`() = runTest {
        val repository = RecordingReporteRepository(Result.failure(IllegalStateException("Sin red")))
        val locationProvider = ControlledLocationProvider()
        val viewModel = ReportarViewModel(repository, locationProvider)
        val fotoUri = mock(Uri::class.java)
        val fix = LocationFix(lat = 19.40, lng = -99.10, accuracyMeters = 14.0)

        viewModel.onFotoCapturada(fotoUri)
        viewModel.obtenerFix()
        runCurrent()
        locationProvider.complete(Result.success(fix))
        advanceUntilIdle()
        viewModel.onSeveridad(Severidad.MODERADO)
        viewModel.onDescripcion("Frente a la entrada")

        viewModel.enviar()
        advanceUntilIdle()

        assertEquals(1, repository.crearCalls.size)
        assertEquals(fotoUri, viewModel.uiState.value.fotoUri)
        assertEquals(fix, viewModel.uiState.value.fix)
        assertEquals(Severidad.MODERADO, viewModel.uiState.value.severidad)
        assertEquals("Frente a la entrada", viewModel.uiState.value.descripcion)
        assertFalse(viewModel.uiState.value.enviando)
        assertFalse(viewModel.uiState.value.enviado)
        assertEquals("Sin red", viewModel.uiState.value.envioError)
        assertTrue(viewModel.uiState.value.puedeEnviar)
    }

    @Test
    fun `submit success emits enviado`() = runTest {
        val repository = RecordingReporteRepository(Result.success("nuevo-id"))
        val locationProvider = ControlledLocationProvider()
        val viewModel = ReportarViewModel(repository, locationProvider)
        val fotoUri = mock(Uri::class.java)
        val fix = LocationFix(lat = 19.40, lng = -99.10, accuracyMeters = 14.0)

        viewModel.onFotoCapturada(fotoUri)
        viewModel.obtenerFix()
        runCurrent()
        locationProvider.complete(Result.success(fix))
        advanceUntilIdle()

        viewModel.enviar()
        advanceUntilIdle()

        assertEquals(1, repository.crearCalls.size)
        assertFalse(viewModel.uiState.value.enviando)
        assertTrue(viewModel.uiState.value.enviado)
        assertFalse(viewModel.uiState.value.puedeEnviar)
    }

    private class ControlledLocationProvider : LocationProvider {
        private var current = CompletableDeferred<Result<LocationFix>>()

        override suspend fun fixActual(): Result<LocationFix> {
            val request = current
            return request.await()
        }

        fun complete(result: Result<LocationFix>) {
            current.complete(result)
            current = CompletableDeferred()
        }
    }

    private data class CrearCall(
        val fotoUri: Uri,
        val fix: LocationFix,
        val severidad: Severidad?,
        val descripcion: String?,
    )

    private class RecordingReporteRepository(
        private val crearResult: Result<String> = Result.success("id"),
    ) : ReporteRepository {

        val crearCalls = mutableListOf<CrearCall>()

        override suspend fun crearReporte(
            fotoUri: Uri,
            fix: LocationFix,
            severidad: Severidad?,
            descripcion: String?,
        ): Result<String> {
            crearCalls += CrearCall(fotoUri, fix, severidad, descripcion)
            return crearResult
        }

        override fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>> =
            throw AssertionError("observarViewport is not used by ReportarViewModel")

        override fun recientes(limit: Int): Flow<List<Reporte>> =
            throw AssertionError("recientes is not used by ReportarViewModel")

        override suspend fun confirmar(reporteId: String): Result<Unit> =
            throw AssertionError("confirmar is not used by ReportarViewModel")

        override suspend fun yaConfirmo(reporteId: String): Boolean =
            throw AssertionError("yaConfirmo is not used by ReportarViewModel")

        override suspend fun eliminar(reporteId: String): Result<Unit> =
            throw AssertionError("eliminar is not used by ReportarViewModel")
    }

    private fun normalized(value: String?): String? =
        value
            ?.replace("\u00f3", "o")
            ?.replace("\u2014", "-")
}
