package com.example.mangos.data.repository

import com.example.mangos.data.model.Supplier
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun observeActive(): Flow<List<Supplier>>

    fun observeAll(): Flow<List<Supplier>>

    suspend fun getById(id: String): Supplier?

    suspend fun add(supplier: Supplier): Result<String>

    suspend fun update(supplier: Supplier): Result<Unit>

    suspend fun deactivate(id: String): Result<Unit>

    suspend fun ensureUnregisteredExists()
}
