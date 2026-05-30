package com.example.mangos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.model.User
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val user: User? = null,
    val todaySummary: PurchaseRepository.TodaySummary = PurchaseRepository.TodaySummary(
        totalTons = 0.0,
        totalSpendCentavos = 0L,
        purchaseCount = 0,
        purchasesWithoutPrice = 0,
    ),
    val activeSupplierCount: Int = 0,
    val recentPurchases: List<DashboardPurchaseUi> = emptyList(),
    val isSigningOut: Boolean = false,
)

data class DashboardPurchaseUi(
    val purchase: Purchase,
    val isPendingSync: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val purchaseRepository: PurchaseRepository,
    supplierRepository: SupplierRepository,
) : ViewModel() {

    private val todayDateKey: String = LocalDate.now(MX_ZONE).toString()

    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.currentUser,
        purchaseRepository.observeRecent(limit = 5),
        supplierRepository.observeActive(),
    ) { user, recentPurchases, activeSuppliers ->
        DashboardUiState(
            user = user,
            todaySummary = purchaseRepository.getTodaySummary(todayDateKey),
            activeSupplierCount = activeSuppliers.count { it.id != Supplier.UNREGISTERED_ID },
            recentPurchases = recentPurchases.map { purchase ->
                DashboardPurchaseUi(purchase = purchase)
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(user = authRepository.currentUser.value),
    )

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    private companion object {
        val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    }
}
