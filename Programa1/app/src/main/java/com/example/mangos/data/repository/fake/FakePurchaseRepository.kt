package com.example.mangos.data.repository.fake

import com.example.mangos.data.model.Purchase
import com.example.mangos.data.repository.PurchaseRepository
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class FakePurchaseRepository @Inject constructor() : PurchaseRepository {

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())

    override fun observeByDateKey(dateKey: String): Flow<List<Purchase>> =
        _purchases.map { list ->
            list.filter { it.dateKey == dateKey && it.deletedAt == null }
                .sortedByDescending { it.enteredAt?.seconds ?: 0L }
        }

    override fun observeBySupplier(supplierId: String, limit: Int): Flow<List<Purchase>> =
        _purchases.map { list ->
            list.asSequence()
                .filter { it.supplierId == supplierId && it.deletedAt == null }
                .sortedByDescending { it.enteredAt?.seconds ?: 0L }
                .take(limit)
                .toList()
        }

    override fun observeRecent(limit: Int): Flow<List<Purchase>> =
        _purchases.map { list ->
            list.asSequence()
                .filter { it.deletedAt == null }
                .sortedByDescending { it.enteredAt?.seconds ?: 0L }
                .take(limit)
                .toList()
        }

    override suspend fun getTodaySummary(dateKey: String): PurchaseRepository.TodaySummary {
        val todays = _purchases.value.filter { it.dateKey == dateKey && it.deletedAt == null }
        val totalTons = todays.sumOf { it.quantityTons }
        val totalSpendCentavos = todays
            .filter { it.pricePerTonCentavos != null }
            .sumOf { (it.pricePerTonCentavos!! * it.quantityTons).toLong() }
        return PurchaseRepository.TodaySummary(
            totalTons = totalTons,
            totalSpendCentavos = totalSpendCentavos,
            purchaseCount = todays.size,
            purchasesWithoutPrice = todays.count { it.pricePerTonCentavos == null },
        )
    }

    override suspend fun add(purchase: Purchase): Result<String> {
        val newId = if (purchase.id.isBlank()) UUID.randomUUID().toString() else purchase.id
        val now = Timestamp.now()
        // TODO(task 11): replace inline LocalDate call with DateKey.toDateKey(purchase.date)
        val computedDateKey = LocalDate.now(ZoneId.of("America/Mexico_City")).toString()
        val toInsert = purchase.copy(
            id = newId,
            enteredAt = now,
            serverWrittenAt = now,
            dateKey = computedDateKey,
        )
        _purchases.value = _purchases.value + toInsert
        return Result.success(newId)
    }

    override suspend fun update(purchase: Purchase): Result<Unit> {
        val current = _purchases.value
        val index = current.indexOfFirst { it.id == purchase.id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Purchase ${purchase.id} not found"))
        }
        _purchases.value = current.toMutableList().also { it[index] = purchase }
        return Result.success(Unit)
    }

    override suspend fun softDelete(id: String, deletedBy: String): Result<Unit> {
        val current = _purchases.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Purchase $id not found"))
        }
        _purchases.value = current.toMutableList().also {
            it[index] = it[index].copy(
                deletedAt = Timestamp.now(),
                deletedBy = deletedBy,
            )
        }
        return Result.success(Unit)
    }
}
