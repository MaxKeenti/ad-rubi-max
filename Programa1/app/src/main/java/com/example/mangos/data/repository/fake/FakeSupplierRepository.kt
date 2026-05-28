package com.example.mangos.data.repository.fake

import com.example.mangos.data.model.Supplier
import com.example.mangos.data.repository.SupplierRepository
import com.google.firebase.Timestamp
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class FakeSupplierRepository @Inject constructor() : SupplierRepository {

    private val seedTimestamp = Timestamp.now()

    private val _suppliers = MutableStateFlow(
        listOf(
            Supplier(
                id = Supplier.UNREGISTERED_ID,
                name = Supplier.UNREGISTERED_NAME,
                phone = "",
                email = "",
                location = "",
                mangoVariety = "",
                isActive = true,
                createdAt = seedTimestamp,
                createdBy = "system",
            ),
            Supplier(
                id = "sup1",
                name = "Hernández y Hermanos",
                phone = "555-1111",
                email = "h@h.com",
                location = "Veracruz",
                mangoVariety = "Ataulfo",
                isActive = true,
                createdAt = seedTimestamp,
                createdBy = "max-uid",
            ),
            Supplier(
                id = "sup2",
                name = "Mangos del Pacífico",
                phone = "555-2222",
                email = "p@m.com",
                location = "Nayarit",
                mangoVariety = "Manila",
                isActive = true,
                createdAt = seedTimestamp,
                createdBy = "max-uid",
            ),
            Supplier(
                id = "sup3",
                name = "Frutas Selectas SA",
                phone = "555-3333",
                email = "f@s.com",
                location = "Oaxaca",
                mangoVariety = "Tommy Atkins",
                isActive = false,
                createdAt = seedTimestamp,
                createdBy = "max-uid",
            ),
        )
    )

    override fun observeActive(): Flow<List<Supplier>> =
        _suppliers.map { list -> list.filter { it.isActive } }

    override fun observeAll(): Flow<List<Supplier>> = _suppliers

    override suspend fun getById(id: String): Supplier? =
        _suppliers.value.firstOrNull { it.id == id }

    override suspend fun add(supplier: Supplier): Result<String> {
        val newId = if (supplier.id.isBlank()) UUID.randomUUID().toString() else supplier.id
        val toInsert = supplier.copy(
            id = newId,
            createdAt = supplier.createdAt ?: Timestamp.now(),
        )
        _suppliers.value = _suppliers.value + toInsert
        return Result.success(newId)
    }

    override suspend fun update(supplier: Supplier): Result<Unit> {
        val current = _suppliers.value
        val index = current.indexOfFirst { it.id == supplier.id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Supplier ${supplier.id} not found"))
        }
        _suppliers.value = current.toMutableList().also { it[index] = supplier }
        return Result.success(Unit)
    }

    override suspend fun deactivate(id: String): Result<Unit> {
        val current = _suppliers.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Supplier $id not found"))
        }
        _suppliers.value = current.toMutableList().also {
            it[index] = it[index].copy(isActive = false)
        }
        return Result.success(Unit)
    }

    override suspend fun ensureUnregisteredExists() {
        if (_suppliers.value.none { it.id == Supplier.UNREGISTERED_ID }) {
            _suppliers.value = _suppliers.value + Supplier(
                id = Supplier.UNREGISTERED_ID,
                name = Supplier.UNREGISTERED_NAME,
                isActive = true,
                createdAt = Timestamp.now(),
                createdBy = "system",
            )
        }
    }
}
