package com.example.bachewatch.data.repository.fake

import com.example.bachewatch.data.auth.FakeSesionAnonima
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
