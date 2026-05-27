# Task 08 — Melanie — Supplier list screen + ViewModel (admin only)

**Owner:** Melanie
**Estimated effort:** ~1 hour
**Prerequisites:** `04-melanie-navigation` (route exists)
**Day:** 3

## Goal

The Admin's CRUD list of suppliers. Hidden from Operators via the
navigation tab filter (already in place from task 04); also enforced
server-side by Firestore rules (task 15).

## Inputs

- `docs/Programa1/entrega/02-requerimientos/content.md` § 3.3 (RF-PROV-01..04)
- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.2
- `docs/Programa1/entrega/07-manual-de-usuario/content.md` § 6
- Repos: `SupplierRepository.observeAll()`, `deactivate()`

## Outputs

- `ui/suppliers/SupplierListScreen.kt` — `LazyColumn` of suppliers. Each row shows name, location, mango variety, and an "Activo"/"Inactivo" chip. FAB to add new. Tap to edit. Swipe to deactivate (with snackbar confirmation).
- `ui/suppliers/SupplierListViewModel.kt` — observes `observeAll()`; filters out `UNREGISTERED` placeholder.

## Acceptance criteria

- [ ] List renders all suppliers except `UNREGISTERED`.
- [ ] FAB → AddEditSupplier screen.
- [ ] Tap → AddEditSupplier in edit mode.
- [ ] Swipe → calls `deactivate()`, the supplier's badge flips to "Inactivo" but stays in the list.
- [ ] CP-08 (deactivated supplier vs historical) — verify the supplier no longer appears in AddEditPurchase's dropdown after deactivation.

## Pitfalls / notes

- **Filter UNREGISTERED out of this list** — admins manage real suppliers; UNREGISTERED is a system placeholder.
- **Show inactive suppliers** (don't filter them out) — admins need to see them to reactivate or audit.
- **Reactivation flow:** opening an inactive supplier in AddEditSupplier and saving should set `isActive = true`. Or add an explicit "Reactivar" button — simpler.
- **No hard-delete UI** — the requirements explicitly say deactivation is the only "remove" gesture.
