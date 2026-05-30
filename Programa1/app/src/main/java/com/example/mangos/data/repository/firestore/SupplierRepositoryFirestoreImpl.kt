package com.example.mangos.data.repository.firestore

import com.example.mangos.data.model.Supplier
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.SupplierRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class SupplierRepositoryFirestoreImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : SupplierRepository {

    override fun observeActive(): Flow<List<Supplier>> =
        firestore.collection(SUPPLIERS)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toSupplier() } }

    override fun observeAll(): Flow<List<Supplier>> =
        firestore.collection(SUPPLIERS)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toSupplier() } }

    override suspend fun getById(id: String): Supplier? {
        return firestore.collection(SUPPLIERS).document(id).get().await().toSupplier()
    }

    override suspend fun add(supplier: Supplier): Result<String> = runCatching {
        val creatorId = authRepository.currentUser.value?.id
            ?: error("No hay sesión activa para crear el proveedor.")
        val collection = firestore.collection(SUPPLIERS)
        val newId = if (supplier.id.isBlank()) collection.document().id else supplier.id
        val data = mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "location" to supplier.location,
            "mangoVariety" to supplier.mangoVariety,
            "isActive" to supplier.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to creatorId,
        )
        collection.document(newId).set(data).await()
        newId
    }

    override suspend fun update(supplier: Supplier): Result<Unit> = runCatching {
        // Omit createdAt/createdBy so the originals are preserved.
        val data = mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "location" to supplier.location,
            "mangoVariety" to supplier.mangoVariety,
            "isActive" to supplier.isActive,
        )
        firestore.collection(SUPPLIERS)
            .document(supplier.id)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun deactivate(id: String): Result<Unit> = runCatching {
        firestore.collection(SUPPLIERS)
            .document(id)
            .update("isActive", false)
            .await()
        Unit
    }

    override suspend fun ensureUnregisteredExists() {
        val docRef = firestore.collection(SUPPLIERS).document(Supplier.UNREGISTERED_ID)
        val snap = docRef.get().await()
        if (snap.exists()) return
        val data = mapOf(
            "name" to "Proveedor no registrado",
            "phone" to "",
            "email" to "",
            "location" to "",
            "mangoVariety" to "",
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to "system",
        )
        docRef.set(data).await()
    }

    private fun DocumentSnapshot.toSupplier(): Supplier? {
        if (!exists()) return null
        return Supplier(
            id = id,
            name = getString("name").orEmpty(),
            phone = getString("phone").orEmpty(),
            email = getString("email").orEmpty(),
            location = getString("location").orEmpty(),
            mangoVariety = getString("mangoVariety").orEmpty(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
            createdBy = getString("createdBy").orEmpty(),
        )
    }

    private companion object {
        const val SUPPLIERS = "suppliers"
    }
}
