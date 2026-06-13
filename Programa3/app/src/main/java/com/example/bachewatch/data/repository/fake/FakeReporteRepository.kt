package com.example.bachewatch.data.repository.fake

import android.net.Uri
import com.example.bachewatch.data.auth.FakeSesionAnonima
import com.example.bachewatch.data.auth.SesionAnonima
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.repository.ReporteRepository
import com.google.firebase.Timestamp
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory ReporteRepository with the same observable semantics the
 * Firestore impl will have (confirmar idempotence, soft-delete rules),
 * so ViewModels and Melanie's demo behave identically (Q15).
 */
@Singleton
class FakeReporteRepository @Inject constructor(
    private val sesion: SesionAnonima,
) : ReporteRepository {

    private val reportes = MutableStateFlow(seedReportes())
    private val confirmadosPorMi = mutableSetOf<String>()

    private fun uidActual(): String = sesion.uid.value ?: FakeSesionAnonima.FAKE_UID

    override suspend fun crearReporte(
        fotoUri: Uri,
        fix: LocationFix,
        severidad: Severidad?,
        descripcion: String?,
    ): Result<String> {
        delay(800) // visible "enviando" state
        val id = UUID.randomUUID().toString()
        val nuevo = Reporte(
            id = id,
            lat = fix.lat,
            lng = fix.lng,
            geohash = "",
            accuracyMeters = fix.accuracyMeters,
            severidad = severidad,
            descripcion = descripcion?.take(200)?.takeIf { it.isNotBlank() },
            fotoPath = "reportes/$id.jpg",
            fotoUrl = "https://picsum.photos/seed/$id/800/600",
            createdBy = uidActual(),
            confirmCount = 0,
            serverWrittenAt = Timestamp.now(),
        )
        reportes.update { it + nuevo }
        return Result.success(id)
    }

    override fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>> =
        reportes.map { lista ->
            lista.filter { it.deletedAt == null && bounds.contains(it.lat, it.lng) }
        }

    override fun recientes(limit: Int): Flow<List<Reporte>> =
        reportes.map { lista ->
            lista.filter { it.deletedAt == null }
                .sortedByDescending { it.serverWrittenAt?.toDate() }
                .take(limit)
        }

    override suspend fun confirmar(reporteId: String): Result<Unit> {
        delay(400)
        if (reportes.value.none { it.id == reporteId && it.deletedAt == null }) {
            return Result.failure(NoSuchElementException("El reporte ya no existe"))
        }
        if (!confirmadosPorMi.add(reporteId)) {
            return Result.failure(IllegalStateException("Ya confirmaste este reporte"))
        }
        reportes.update { lista ->
            lista.map {
                if (it.id == reporteId) it.copy(confirmCount = it.confirmCount + 1) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun yaConfirmo(reporteId: String): Boolean = reporteId in confirmadosPorMi

    override suspend fun eliminar(reporteId: String): Result<Unit> {
        delay(400)
        val uid = uidActual()
        val reporte = reportes.value.firstOrNull { it.id == reporteId && it.deletedAt == null }
            ?: return Result.failure(NoSuchElementException("El reporte ya no existe"))
        if (reporte.createdBy != uid) {
            return Result.failure(SecurityException("Solo el creador puede eliminar"))
        }
        val edadMs = System.currentTimeMillis() - (reporte.serverWrittenAt?.toDate()?.time ?: 0L)
        if (edadMs > 24L * 60 * 60 * 1_000) {
            return Result.failure(IllegalStateException("La ventana de 24 horas ya venció"))
        }
        reportes.update { lista ->
            lista.map {
                if (it.id == reporteId) it.copy(deletedAt = Timestamp.now(), deletedBy = uid) else it
            }
        }
        return Result.success(Unit)
    }
}
