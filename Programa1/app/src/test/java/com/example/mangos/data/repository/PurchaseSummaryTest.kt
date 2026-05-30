package com.example.mangos.data.repository

import com.example.mangos.data.model.Purchase
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseSummaryTest {

    @Test
    fun `toTodaySummary excludes deleted rows and summarizes price-less purchases`() {
        val summary = listOf(
            purchase(id = "priced", quantityTons = 2.5, pricePerTonCentavos = 10_000L),
            purchase(id = "price-less", quantityTons = 1.0, pricePerTonCentavos = null),
            purchase(
                id = "deleted",
                quantityTons = 9.0,
                pricePerTonCentavos = 1_000L,
                deletedAt = Timestamp(10, 0),
            ),
        ).toTodaySummary()

        assertEquals(3.5, summary.totalTons, 0.0)
        assertEquals(25_000L, summary.totalSpendCentavos)
        assertEquals(2, summary.purchaseCount)
        assertEquals(1, summary.purchasesWithoutPrice)
    }

    private fun purchase(
        id: String,
        quantityTons: Double,
        pricePerTonCentavos: Long?,
        deletedAt: Timestamp? = null,
    ): Purchase {
        val now = Timestamp(1, 0)
        return Purchase(
            id = id,
            supplierId = "supplier-1",
            supplierName = "Proveedor",
            quantityTons = quantityTons,
            pricePerTonCentavos = pricePerTonCentavos,
            date = now,
            dateKey = "2026-05-30",
            createdBy = "operator-1",
            createdByName = "Operator",
            enteredAt = now,
            serverWrittenAt = now,
            deletedAt = deletedAt,
        )
    }
}
