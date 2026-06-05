# Task 07 — Melanie — Purchase history screen + ViewModel

**Owner:** Melanie
**Estimated effort:** ~1 hour
**Prerequisites:** `06-melanie-add-edit-purchase` (need the AddEdit route for tap-to-edit)
**Day:** 2

## Goal

A scrollable list of all live (non-deleted) purchases, newest first, with
a supplier filter chip row. Tap to edit (subject to edit-window rules).
Swipe to soft-delete (subject to same rules).

## Inputs

- `docs/entrega/02-requerimientos/content.md` § 3.2 RF-COMPRA-07 + § 3.4 RF-DASH
- `docs/entrega/07-manual-de-usuario/content.md` § 5
- Repos: `PurchaseRepository.observeBySupplier()`, `observeRecent()` (use a higher limit), `softDelete()`
- Utils: `MoneyFormatter`

## Outputs

- `ui/purchases/PurchaseHistoryScreen.kt` — `LazyColumn` of purchases. Top: horizontal scrollable `Row` of `FilterChip`s for suppliers (one chip per active supplier, plus a "Todos" chip). Each item shows supplier name, quantity, price (or "—"), `date`, and a small `enteredAt` timestamp.
- `ui/purchases/PurchaseHistoryViewModel.kt` — exposes a `selectedSupplierId: StateFlow<String?>` and a `purchases: StateFlow<List<Purchase>>` that switches source based on the filter.

## Acceptance criteria

- [ ] List renders, newest first.
- [ ] Filter chips switch the visible list.
- [ ] Tap → opens AddEditPurchase in edit mode (subject to canEdit).
- [ ] Swipe-to-delete → calls `softDelete()` → row disappears from the list.
- [ ] CP-06 (soft-delete) passes — verify in Firestore that the doc still exists with `deletedAt != null`.

## Pitfalls / notes

- **50-item limit per fetch** — implement "Cargar más" if you have time; otherwise hard-cap at 50.
- **Swipe-to-delete in Compose** = `SwipeToDismissBox` (Material 3). Confirm with a snackbar that has "Deshacer" within 5s? Nice-to-have, skip if tight.
- **Deleted rows don't reappear** — `observeBy*` filters server-side via `deletedAt == null`.
- **No date-range filter** — explicitly cut from scope; do not add one.
- **Pull-to-refresh:** not required (Firestore listeners auto-update).
