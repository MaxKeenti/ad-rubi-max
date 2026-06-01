# Task 05 — Melanie — Dashboard screen + ViewModel

**Owner:** Melanie
**Estimated effort:** ~2 hours
**Prerequisites:** `04-melanie-navigation`
**Day:** 2

## Goal

The home screen. Shows today's totals, recent purchases with pending-sync
indicators, a FAB to add a purchase, and the logout overflow.

## Inputs

- `docs/entrega/02-requerimientos/content.md` § 3.4 (RF-DASH-01..04)
- `docs/entrega/04-modelo-de-datos/content.md` § 2.3 (Purchase fields, `dateKey`)
- `docs/entrega/07-manual-de-usuario/content.md` § 3
- Repository: `PurchaseRepository.observeByDateKey()`, `observeRecentWithPending()`, `SupplierRepository.observeActive()`, `AuthRepository.currentUser` + `signOut()`
- Summary helper: `Iterable<Purchase>.toTodaySummary()`
- Util: `todayDateKey()`, `MoneyFormatter`

## Outputs

- `ui/dashboard/DashboardScreen.kt` — Composable:
  - Top bar: greeting `"Hola, ${user.displayName}"` + date + overflow `(⋮)` menu containing "Cerrar sesión"
  - 3 summary cards: total toneladas hoy, número de compras hoy, proveedores activos
  - "Últimas 5 compras" list — supplier, tons, time (from `enteredAt`), **pending-sync badge** if Firestore says `hasPendingWrites`
  - FAB: "+" navigates to AddEditPurchase
- `ui/dashboard/DashboardViewModel.kt` — combines `observeByDateKey(today).map { it.toTodaySummary() }` + `observeRecentWithPending(5)` + `observeActive()` into a single `uiState`.

## Acceptance criteria

- [ ] CP-02 partial (creation reflected in dashboard) passes against Fake.
- [ ] Adding a purchase via FAB → Dashboard reflects new totals on return.
- [ ] Logout from overflow → returns to Login.
- [ ] Pending-sync badge renders (against Fake it never fires; against real repo it will when offline).

## Pitfalls / notes

- **`hasPendingWrites`** comes from Firestore's `DocumentSnapshot.metadata.hasPendingWrites`. The repository should expose a wrapper `Purchase` + `Boolean isPending` pair, OR a separate Flow of pending ids. Discuss with Max if the interface needs adjustment — but only via an interface re-open.
- **Date display:** "26 de mayo de 2026" in the greeting card, format with `Locale("es", "MX")` and `DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)`.
- **Empty state:** if today has zero purchases, show "Aún no se han registrado entradas hoy." Not just zero.
- **Pull-to-refresh:** not required for v1 (Firestore listeners auto-update).
- **The FAB is the most-used action of the entire app** — make it big and high-contrast.
