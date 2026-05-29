package com.example.mangos.ui.reports

import androidx.lifecycle.ViewModel
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.util.todayDateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class ReportsUiState(
    val todaySummary: PurchaseRepository.TodaySummary = PurchaseRepository.TodaySummary(
        totalTons = 0.0,
        totalSpendCentavos = 0L,
        purchaseCount = 0,
        purchasesWithoutPrice = 0,
    ),
    val topSuppliers: List<TopSupplierUi> = emptyList(),
)

data class TopSupplierUi(
    val supplierName: String,
    val totalTons: Double,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
) : ViewModel() {

    private val todayKey = todayDateKey(MX_ZONE)
    private val monthStartKey = LocalDate.now(MX_ZONE)
        .withDayOfMonth(1)
        .toString()
    private val nextMonthStartKey = LocalDate.parse(monthStartKey)
        .plusMonths(1)
        .toString()

    val uiState: StateFlow<ReportsUiState> = purchaseRepository
        .observeByDateRange(monthStartKey, nextMonthStartKey)
        .map { monthPurchases ->
            ReportsUiState(
                todaySummary = purchaseRepository.getTodaySummary(todayKey),
                topSuppliers = monthPurchases.toTopSuppliers(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsUiState(),
        )

    private fun List<Purchase>.toTopSuppliers(): List<TopSupplierUi> =
        asSequence()
            .filter { it.supplierId != Supplier.UNREGISTERED_ID }
            .groupBy { it.supplierId }
            .values
            .map { purchases ->
                TopSupplierUi(
                    supplierName = purchases.first().supplierName,
                    totalTons = purchases.sumOf { it.quantityTons },
                )
            }
            .sortedByDescending { it.totalTons }
            .take(TOP_SUPPLIER_LIMIT)
            .toList()

    private companion object {
        const val TOP_SUPPLIER_LIMIT = 5
        val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    }
}
