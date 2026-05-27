# Task 06 — Melanie — AddEdit Purchase screen + ViewModel

**Owner:** Melanie
**Estimated effort:** ~2 hours
**Prerequisites:** `05-melanie-dashboard`, `02-melanie-data-models`
**Day:** 2

## Goal

The single most important screen of the app: the dock-side purchase
capture form. Must work offline, support UNREGISTERED supplier with
freeform note, accept optional price, and enforce the 24h edit window
client-side (server-side is enforced by rules).

## Inputs

- `docs/Programa1/entrega/02-requerimientos/content.md` § 3.2 (RF-COMPRA-01..07)
- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.3 (full schema), § 5 (Long centavos)
- `docs/Programa1/entrega/06-glosario/content.md` (UNREGISTERED, three timestamps)
- `docs/Programa1/entrega/07-manual-de-usuario/content.md` § 4
- Repos: `SupplierRepository.observeActive()`, `PurchaseRepository.add()` / `update()`
- Utils: `MoneyFormatter.parseMxnToCentavos`, `DateKey.toDateKey`

## Outputs

- `ui/purchases/AddEditPurchaseScreen.kt`:
  - Supplier dropdown (exposed dropdown menu). Last entry is **"Proveedor no registrado"** (id == `UNREGISTERED_ID`).
  - When UNREGISTERED selected → reveal `OutlinedTextField` for `supplierNoteFreeform`.
  - Quantity (toneladas) field — `KeyboardType.Decimal`, required, must parse to `Double > 0`.
  - Price per ton (MXN) field — `KeyboardType.Decimal`, **optional**. Empty → `pricePerTonCentavos = null`.
  - Date picker, default = today. Allow back-dating.
  - Save button.
  - In **edit mode** (purchaseId != null), pre-fill from `repo.observe...` lookup; disable save if the 24h client check rejects (Operator only — admins always allowed).
- `ui/purchases/AddEditPurchaseViewModel.kt`:
  - `onSave()` builds the `Purchase` object: generates a UUID for id (Firestore will accept it), sets `enteredAt = Timestamp.now()`, leaves `serverWrittenAt` to be set by the repo at write-time, computes `dateKey = purchase.date.toDateKey()`.
  - `canEdit(purchase, user): Boolean` — admin always; operator only if `purchase.createdBy == user.id && now - purchase.serverWrittenAt < 24h`.

## Acceptance criteria

- [ ] CP-02 (normal capture) passes.
- [ ] CP-03 (capture without price) passes — save succeeds, `pricePerTonCentavos == null`.
- [ ] CP-04 (capture against UNREGISTERED) passes — `supplierNoteFreeform` populated.
- [ ] CP-05 (24h edit window) client-side check works (server enforcement is task 15).
- [ ] CP-07 (offline capture) works against Fake — UI doesn't block on network.

## Pitfalls / notes

- **The form validates progressively** — don't error-shake every field on every keystroke. Show field errors on blur or on save attempt.
- **The DatePicker in Material 3** is `androidx.compose.material3.DatePicker` — uses `DatePickerState` and `DatePickerDialog`. Annoying API; read docs.
- **The supplier dropdown** should be searchable if there are many suppliers — but v1 has ~3, so plain dropdown is fine.
- **UNREGISTERED as the last entry**, visually separated by a `HorizontalDivider()` from real suppliers.
- **`enteredAt` is the client clock** — set it in the VM, not in the repo. The repo sets `serverWrittenAt`.
- **`dateKey` recomputes on `date` edit** — your VM must call `purchase.date.toDateKey()` again if the user changes the date in edit mode.
- **Don't validate against the 24h window in the screen** — read it from the VM helper. Single source of truth.
