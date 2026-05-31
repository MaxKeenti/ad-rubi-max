package com.example.mangos.data.repository.firestore

import android.util.Log
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import com.example.mangos.data.repository.toTodaySummary
import com.example.mangos.data.util.toDateKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class PurchaseRepositoryFirestoreImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val supplierRepository: SupplierRepository,
) : PurchaseRepository {

    override fun observeById(id: String): Flow<Purchase?> =
        firestore.collection(PURCHASES)
            .document(id)
            .snapshots()
            .map { snap ->
                snap.toPurchase()?.takeIf { it.deletedAt == null }
            }

    override fun observeByDateKey(dateKey: String): Flow<List<Purchase>> =
        firestore.collection(PURCHASES)
            .whereEqualTo("dateKey", dateKey)
            .whereEqualTo("deletedAt", null)
            .orderBy("enteredAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPurchase() } }

    override fun observeByDateRange(
        startDateKeyInclusive: String,
        endDateKeyExclusive: String,
    ): Flow<List<Purchase>> =
        firestore.collection(PURCHASES)
            .whereGreaterThanOrEqualTo("dateKey", startDateKeyInclusive)
            .whereLessThan("dateKey", endDateKeyExclusive)
            .whereEqualTo("deletedAt", null)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toPurchase() }
                    .sortedByDescending { it.enteredAt.seconds }
            }

    override fun observeBySupplier(supplierId: String, limit: Int): Flow<List<Purchase>> =
        firestore.collection(PURCHASES)
            .whereEqualTo("supplierId", supplierId)
            .whereEqualTo("deletedAt", null)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPurchase() } }

    override fun observeRecent(limit: Int): Flow<List<Purchase>> =
        firestore.collection(PURCHASES)
            .whereEqualTo("deletedAt", null)
            .orderBy("enteredAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPurchase() } }

    override fun observeRecentWithPending(
        limit: Int,
    ): Flow<List<PurchaseRepository.PendingAware>> =
        firestore.collection(PURCHASES)
            .whereEqualTo("deletedAt", null)
            .orderBy("enteredAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            // INCLUDE metadata changes so the pending→synced flip
            // (hasPendingWrites: true→false) emits an update and the badge
            // clears. EXCLUDE would swallow that metadata-only transition.
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val purchase = doc.toPurchase() ?: return@mapNotNull null
                    PurchaseRepository.PendingAware(
                        purchase = purchase,
                        isPending = doc.metadata.hasPendingWrites(),
                    )
                }
            }

    override suspend fun getTodaySummary(dateKey: String): PurchaseRepository.TodaySummary {
        val snap = firestore.collection(PURCHASES)
            .whereEqualTo("dateKey", dateKey)
            .whereEqualTo("deletedAt", null)
            .get()
            .await()
        return snap.documents.mapNotNull { it.toPurchase() }.toTodaySummary()
    }

    override suspend fun add(purchase: Purchase): Result<String> = runCatching {
        val creator = authRepository.currentUser.value
            ?: error("No hay sesión activa para registrar la compra.")

        // Defensive denormalization: prefer the latest cached supplier name and
        // the authenticated user's displayName. Fall back to what the caller
        // already populated if a lookup misses.
        val supplierName = runCatching { supplierRepository.getById(purchase.supplierId) }
            .getOrNull()
            ?.name
            ?.takeIf { it.isNotBlank() }
            ?: purchase.supplierName
        val createdByName = creator.displayName.takeIf { it.isNotBlank() }
            ?: purchase.createdByName

        val collection = firestore.collection(PURCHASES)
        val newId = if (purchase.id.isBlank()) collection.document().id else purchase.id

        val data: Map<String, Any?> = mapOf(
            "supplierId" to purchase.supplierId,
            "supplierName" to supplierName,
            "supplierNoteFreeform" to purchase.supplierNoteFreeform,
            "quantityTons" to purchase.quantityTons,
            "pricePerTonCentavos" to purchase.pricePerTonCentavos,
            "date" to purchase.date,
            "dateKey" to purchase.date.toDateKey(),
            "createdBy" to creator.id,
            "createdByName" to createdByName,
            "enteredAt" to purchase.enteredAt,
            "serverWrittenAt" to FieldValue.serverTimestamp(),
            "deletedAt" to null,
            "deletedBy" to null,
        )
        // Fire-and-forget: the Task returned by set() only resolves on
        // server ack, so .await() would hang indefinitely offline and leave
        // the form's isSaving stuck. The local cache persists synchronously
        // and snapshot listeners pick it up immediately; the server ack is
        // observable later via metadata.hasPendingWrites flipping to false.
        collection.document(newId).set(data)
            .addOnFailureListener { ex ->
                Log.e(TAG, "set() failed for purchase=$newId", ex)
            }
        newId
    }

    override suspend fun update(purchase: Purchase): Result<Unit> = runCatching {
        // Omit serverWrittenAt, createdBy, createdByName, supplierName, enteredAt
        // so the originals are preserved (history is a snapshot in time, ADR-0002
        // anchors the 24h window on serverWrittenAt — never overwrite it).
        val data: Map<String, Any?> = mapOf(
            "supplierId" to purchase.supplierId,
            "supplierNoteFreeform" to purchase.supplierNoteFreeform,
            "quantityTons" to purchase.quantityTons,
            "pricePerTonCentavos" to purchase.pricePerTonCentavos,
            "date" to purchase.date,
            "dateKey" to purchase.date.toDateKey(),
        )
        firestore.collection(PURCHASES)
            .document(purchase.id)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun softDelete(id: String, deletedBy: String): Result<Unit> = runCatching {
        // Client-side Timestamp.now() (not FieldValue.serverTimestamp()) so the
        // local cache projects a non-null deletedAt immediately. With the
        // server sentinel, pending writes leave deletedAt = null in the cache
        // and whereEqualTo("deletedAt", null) keeps matching the doc until
        // the server roundtrip lands — which is the CP-06 refresh bug. The
        // authoritative audit clock remains serverWrittenAt (ADR-0002);
        // deletedAt is informative.
        val updates: Map<String, Any> = mapOf(
            "deletedAt" to Timestamp.now(),
            "deletedBy" to deletedBy,
        )
        firestore.collection(PURCHASES)
            .document(id)
            .update(updates)
            .await()
        Unit
    }

    private fun DocumentSnapshot.toPurchase(): Purchase? {
        if (!exists()) return null
        val enteredAt = getTimestamp("enteredAt") ?: return null
        return Purchase(
            id = id,
            supplierId = getString("supplierId").orEmpty(),
            supplierName = getString("supplierName").orEmpty(),
            supplierNoteFreeform = getString("supplierNoteFreeform"),
            quantityTons = getDouble("quantityTons") ?: 0.0,
            pricePerTonCentavos = getLong("pricePerTonCentavos"),
            date = getTimestamp("date") ?: enteredAt,
            dateKey = getString("dateKey").orEmpty(),
            createdBy = getString("createdBy").orEmpty(),
            createdByName = getString("createdByName").orEmpty(),
            enteredAt = enteredAt,
            // serverWrittenAt is null in cached/pending snapshots until the
            // server timestamp resolves. Fall back to enteredAt so the local
            // 24h-edit-window check still works while the write is queued.
            serverWrittenAt = getTimestamp("serverWrittenAt") ?: enteredAt,
            deletedAt = getTimestamp("deletedAt"),
            deletedBy = getString("deletedBy"),
        )
    }

    private companion object {
        const val PURCHASES = "purchases"
        const val TAG = "PurchaseRepoFirestore"
    }
}
