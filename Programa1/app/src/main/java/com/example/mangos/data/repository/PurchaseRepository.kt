package com.example.mangos.data.repository

import com.example.mangos.data.model.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    fun observeById(id: String): Flow<Purchase?>

    fun observeByDateKey(dateKey: String): Flow<List<Purchase>>

    fun observeByDateRange(startDateKeyInclusive: String, endDateKeyExclusive: String): Flow<List<Purchase>>

    fun observeBySupplier(supplierId: String, limit: Int = 50): Flow<List<Purchase>>

    fun observeRecent(limit: Int = 5): Flow<List<Purchase>>

    fun observeRecentWithPending(limit: Int = 5): Flow<List<PendingAware>>

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

    data class PendingAware(
        val purchase: Purchase,
        val isPending: Boolean,
    )
}

fun Iterable<Purchase>.toTodaySummary(): PurchaseRepository.TodaySummary {
    val livePurchases = filter { it.deletedAt == null }
    val totalSpendCentavos = livePurchases
        .filter { it.pricePerTonCentavos != null }
        .sumOf { (it.pricePerTonCentavos!! * it.quantityTons).toLong() }

    return PurchaseRepository.TodaySummary(
        totalTons = livePurchases.sumOf { it.quantityTons },
        totalSpendCentavos = totalSpendCentavos,
        purchaseCount = livePurchases.size,
        purchasesWithoutPrice = livePurchases.count { it.pricePerTonCentavos == null },
    )
}
