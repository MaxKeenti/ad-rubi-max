package com.example.bachewatch.data.repository.firestore

import android.net.Uri
import android.util.Log
import com.example.bachewatch.data.auth.SesionAnonima
import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.LocationFix
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.data.util.FotoCompressor
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * The data centerpiece (implementation_plan §2, ADR-0002). UI never sees
 * Firebase/GeoFire types — everything below the [ReporteRepository] contract.
 */
@Singleton
class ReporteRepositoryFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sesion: SesionAnonima,
    private val compressor: FotoCompressor,
) : ReporteRepository {

    override suspend fun crearReporte(
        fotoUri: Uri,
        fix: LocationFix,
        tipo: TipoIncidencia,
        severidad: Severidad?,
        descripcion: String?,
    ): Result<String> = runCatching {
        val uid = sesion.ensureSignedIn().getOrThrow()
        val coleccion = firestore.collection(REPORTES)
        val id = coleccion.document().id
        val fotoPath = "$REPORTES/$id.jpg"

        // Upload-then-write (Q8): photo first, doc only on upload success. An
        // orphan photo on partial failure is the accepted artifact.
        val jpeg = compressor.comprimir(fotoUri)
        val ref = storage.reference.child(fotoPath)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(jpeg, metadata).await()
        val fotoUrl = ref.downloadUrl.await().toString()

        val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(fix.lat, fix.lng))

        val data = buildMap<String, Any?> {
            put("tipo", tipo.valor)
            put("lat", fix.lat)
            put("lng", fix.lng)
            put("geohash", geohash)
            put("accuracyMeters", fix.accuracyMeters)
            // Absent when null (never the literal "null"); rules validate the enum.
            severidad?.let { put("severidad", it.valor) }
            descripcion?.take(200)?.takeIf { it.isNotBlank() }?.let { put("descripcion", it) }
            put("fotoPath", fotoPath)
            put("fotoUrl", fotoUrl)
            put("createdBy", uid)
            put("confirmCount", 0L)
            put("serverWrittenAt", FieldValue.serverTimestamp())
        }
        coleccion.document(id).set(data).await()
        id
    }

    override fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>> {
        // Zoom-out guard (ADR-0002): city-wide, skip unbounded geo-queries and
        // serve the recientes set filtered to the box.
        if (diagonalKm(bounds) > UMBRAL_ZOOM_KM) {
            return recientes(RECIENTES_FALLBACK).map { filtrarViewport(it, bounds) }
        }
        return callbackFlow {
            val centro = GeoLocation(
                (bounds.swLat + bounds.neLat) / 2,
                (bounds.swLng + bounds.neLng) / 2,
            )
            val radioM = GeoFireUtils.getDistanceBetween(
                centro,
                GeoLocation(bounds.neLat, bounds.neLng),
            )
            val limites = GeoFireUtils.getGeoHashQueryBounds(centro, radioM)

            // Snapshot callbacks fire on the main thread, so this map needs no lock.
            val resultados = HashMap<Int, List<Reporte>>()
            val registros = limites.mapIndexed { indice, limite ->
                firestore.collection(REPORTES)
                    .orderBy("geohash")
                    .startAt(limite.startHash)
                    .endAt(limite.endHash)
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            // One failing sub-query shouldn't blank the whole map.
                            Log.w(TAG, "sub-consulta de viewport falló", error)
                            return@addSnapshotListener
                        }
                        if (snap == null) return@addSnapshotListener
                        resultados[indice] = snap.documents.mapNotNull { it.toReporte() }
                        trySend(filtrarViewport(resultados.values.flatten(), bounds))
                    }
            }
            awaitClose { registros.forEach { it.remove() } }
        }
    }

    override fun recientes(limit: Int): Flow<List<Reporte>> =
        firestore.collection(REPORTES)
            .orderBy("serverWrittenAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { snap ->
                // Filter deletedAt client-side: whereEqualTo(null)+orderBy needs a
                // composite index and skips field-absent docs (P1 lesson).
                snap.documents.mapNotNull { it.toReporte() }.filter { it.deletedAt == null }
            }

    override suspend fun confirmar(reporteId: String): Result<Unit> = runCatching {
        val uid = sesion.ensureSignedIn().getOrThrow()
        val reporteRef = firestore.collection(REPORTES).document(reporteId)
        val confirmacionRef = reporteRef.collection(CONFIRMACIONES).document(uid)

        if (confirmacionRef.get().await().exists()) {
            error("Ya confirmaste este reporte")
        }
        // One batch: the uid-keyed confirmación + the count increment (Q11).
        val batch = firestore.batch()
        batch.set(confirmacionRef, mapOf("confirmedAt" to FieldValue.serverTimestamp()))
        batch.update(reporteRef, "confirmCount", FieldValue.increment(1L))
        batch.commit().await()
        Unit
    }

    override suspend fun yaConfirmo(reporteId: String): Boolean {
        val uid = sesion.uid.value ?: sesion.ensureSignedIn().getOrNull() ?: return false
        return runCatching {
            firestore.collection(REPORTES).document(reporteId)
                .collection(CONFIRMACIONES).document(uid)
                .get().await().exists()
        }.getOrDefault(false)
    }

    override suspend fun eliminar(reporteId: String): Result<Unit> = runCatching {
        val uid = sesion.ensureSignedIn().getOrThrow()
        // Set exactly deletedAt/deletedBy; the 24 h + ownership checks live in the
        // rules (task 11), not the client alone (Q12).
        firestore.collection(REPORTES).document(reporteId)
            .update(
                mapOf(
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "deletedBy" to uid,
                ),
            )
            .await()
        Unit
    }

    private fun diagonalKm(bounds: GeoBounds): Double =
        GeoFireUtils.getDistanceBetween(
            GeoLocation(bounds.swLat, bounds.swLng),
            GeoLocation(bounds.neLat, bounds.neLng),
        ) / 1000.0

    private fun DocumentSnapshot.toReporte(): Reporte? {
        if (!exists()) return null
        val lat = getDouble("lat") ?: return null
        val lng = getDouble("lng") ?: return null
        return Reporte(
            id = id,
            tipo = TipoIncidencia.fromValor(getString("tipo")),
            lat = lat,
            lng = lng,
            geohash = getString("geohash").orEmpty(),
            accuracyMeters = getDouble("accuracyMeters") ?: 0.0,
            severidad = Severidad.fromValor(getString("severidad")),
            descripcion = getString("descripcion"),
            fotoPath = getString("fotoPath").orEmpty(),
            fotoUrl = getString("fotoUrl").orEmpty(),
            createdBy = getString("createdBy").orEmpty(),
            confirmCount = getLong("confirmCount") ?: 0L,
            serverWrittenAt = getTimestamp("serverWrittenAt"),
            deletedAt = getTimestamp("deletedAt"),
            deletedBy = getString("deletedBy"),
        )
    }

    private companion object {
        const val REPORTES = "reportes"
        const val CONFIRMACIONES = "confirmaciones"
        const val TAG = "ReporteRepoFirestore"
        const val UMBRAL_ZOOM_KM = 30.0
        const val RECIENTES_FALLBACK = 200
    }
}
