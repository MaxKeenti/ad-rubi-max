# Task 09 — Melanie — AddEdit Supplier screen + ViewModel (admin only)

**Owner:** Melanie
**Estimated effort:** ~30 min
**Prerequisites:** `08-melanie-supplier-list`
**Day:** 3

## Goal

The Admin form for creating or editing a supplier. Simpler than
AddEditPurchase — no offline considerations, no edit window, no
UNREGISTERED.

## Inputs

- `docs/entrega/02-requerimientos/content.md` § 3.3
- `docs/entrega/04-modelo-de-datos/content.md` § 2.2
- Repo: `SupplierRepository.add()`, `update()`

## Outputs

- `ui/suppliers/AddEditSupplierScreen.kt` — form fields:
  - Nombre — required, non-empty
  - Teléfono — optional, basic format hint
  - Correo — optional, email keyboard type
  - Ubicación — required (estado o región)
  - Variedad de mango — required
  - Activo (Switch) — defaults to `true` for new, current value for edit
- `ui/suppliers/AddEditSupplierViewModel.kt` — straightforward state + save.

## Acceptance criteria

- [ ] Create flow: fill in required fields, save → returns to list, new supplier appears.
- [ ] Edit flow: pre-fills, save updates the document.
- [ ] Reactivation: toggling Activo switch on an inactive supplier and saving updates `isActive = true`.
- [ ] CP-08 partial (deactivation visibility on dock dropdown) verifiable after combined with task 06.

## Pitfalls / notes

- **No password, no API key, nothing sensitive** — this is plain supplier metadata.
- **`createdBy` and `createdAt` stay frozen on edit** — only set on create.
- **Don't allow editing `id`** — Firestore document id is immutable.
