package com.example.bachewatch.data.repository.fake

import android.net.Uri
import com.example.bachewatch.data.auth.FakeSesionAnonima
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.TipoIncidencia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class FakeReporteRepositoryTest {

    @Test
    fun `confirmar increments once and then becomes idempotent for the same user`() = runTest {
        val repository = FakeReporteRepository(FakeSesionAnonima())
        val original = repository.recientes().first().first { it.id == "seed-02" }

        val firstResult = repository.confirmar("seed-02")
        val secondResult = repository.confirmar("seed-02")

        val updated = repository.recientes().first().first { it.id == "seed-02" }
        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isFailure)
        assertEquals(original.confirmCount + 1, updated.confirmCount)
        assertTrue(repository.yaConfirmo("seed-02"))
    }

    @Test
    fun `eliminar only soft deletes own reports inside the twenty four hour window`() = runTest {
        val repository = FakeReporteRepository(FakeSesionAnonima())

        val ownRecentResult = repository.eliminar("seed-01")
        val notOwnResult = repository.eliminar("seed-02")
        val ownExpiredResult = repository.eliminar("seed-04")

        val visibleIds = repository.recientes().first().map { it.id }
        assertTrue(ownRecentResult.isSuccess)
        assertFalse("seed-01" in visibleIds)
        assertTrue(notOwnResult.exceptionOrNull() is SecurityException)
        assertTrue(ownExpiredResult.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `crearReporte stores the selected tipo`() = runTest {
        val repository = FakeReporteRepository(FakeSesionAnonima())

        val id = repository.crearReporte(
            fotoUri = mock(Uri::class.java),
            fix = LocationFix(lat = 19.40, lng = -99.10, accuracyMeters = 10.0),
            tipo = TipoIncidencia.OTRO,
            severidad = null,
            descripcion = "Coladera sin tapa",
        ).getOrThrow()

        val created = repository.recientes().first().first { it.id == id }
        assertEquals(TipoIncidencia.OTRO, created.tipo)
    }
}
