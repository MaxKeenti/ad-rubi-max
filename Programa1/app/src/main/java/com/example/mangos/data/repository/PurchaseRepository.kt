package com.example.mangos.data.repository

import com.example.mangos.data.model.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    fun observeById(id: String): Flow<Purchase?>

    fun observeByDateKey(dateKey: String): Flow<List<Purchase>>

    fun observeByDateRange(startDateKeyInclusive: String, endDateKeyExclusive: String): Flow<List<Purchase>>

    fun observeBySupplier(supplierId: String, limit: Int = 50): Flow<List<Purchase>>

    fun observeRecent(limit: Int = 5): Flow<List<Purchase>>

    suspend fun getTodaySummary(dateKey: String): TodaySummary

    suspend fun add(purchase: Purchase): Result<String>

    suspend fun update(purchase: Purchase): Result<Unit>

    suspend fun softDelete(id: String, deletedBy: String): Result<Unit>

    data class TodaySummary(
        val totalTons: Double,
        val totalSpendCentavos: Long,
        val purchaseCount: Int,
        val purchasesWithoutPrice: Int,
    )
}
