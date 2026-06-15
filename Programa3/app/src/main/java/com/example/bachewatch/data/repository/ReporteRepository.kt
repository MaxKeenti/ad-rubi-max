package com.example.bachewatch.data.repository

import android.net.Uri
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import kotlinx.coroutines.flow.Flow

/**
 * Whole-domain contract (grilling Q15). Compression, Storage upload,
 * geohash plumbing all hide behind it; UI never sees Firebase/GeoFire
 * types. Flows are realtime listeners; filtering `deletedAt == null`
 * is the repository's responsibility, not the caller's (P1 lesson).
 */
interface ReporteRepository {

    /**
     * Upload-then-write (Q8): photo first, doc only on upload success.
     * Returns the new reporte id. Failure leaves the composed report
     * for in-place retry (Q7).
     */
    suspend fun crearReporte(
        fotoUri: Uri,
        fix: LocationFix,
        tipo: TipoIncidencia,
        severidad: Severidad?,
        descripcion: String?,
    ): Result<String>

    fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>>

    /** Newest-first, `orderBy serverWrittenAt desc` — ignores viewport. */
    fun recientes(limit: Int = 50): Flow<List<Reporte>>

    /** "Sigue ahí": uid-keyed confirmación + batched count increment (Q11). */
    suspend fun confirmar(reporteId: String): Result<Unit>

    /** Whether this install already confirmed; drives the locked button. */
    suspend fun yaConfirmo(reporteId: String): Boolean

    /** Creator-only soft delete within 24 h of serverWrittenAt (Q12). */
    suspend fun eliminar(reporteId: String): Result<Unit>
}
