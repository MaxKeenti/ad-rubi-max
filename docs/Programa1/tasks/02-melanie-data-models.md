# Task 02 — Melanie — Data model classes

**Owner:** Melanie
**Estimated effort:** ~30 min
**Prerequisites:** `00-max-draft-interfaces` (interfaces reference the models)
**Day:** 1

## Goal

Pure Kotlin `data class` declarations matching the Firestore schemas.
No logic, no behavior. Just the shape of the data that flows through
the repositories.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2 (all three collections)
- `docs/Programa1/entrega/06-glosario/content.md` (semantics of each field)
- `Programa1/app/src/main/java/com/example/mangos/data/repository/*.kt` (interfaces already reference these classes — check the signatures Max drafted)

## Outputs

In `Programa1/app/src/main/java/com/example/mangos/data/model/`:

- `UserRole.kt` — `enum class UserRole { ADMIN, OPERATOR }`. Add `fun fromFirestoreString(s: String?): UserRole` companion helper for the case-sensitive `"admin"` / `"operator"` strings from Firestore.
- `User.kt` — `data class User(id, email, displayName, role: UserRole, accountCreatedAt: Timestamp)`.
- `Supplier.kt` — `data class Supplier(id, name, phone, email, location, mangoVariety, isActive, createdAt: Timestamp, createdBy)`. Add a companion `const val UNREGISTERED_ID = "UNREGISTERED"`.
- `Purchase.kt` — `data class Purchase(id, supplierId, supplierName, supplierNoteFreeform: String?, quantityTons: Double, pricePerTonCentavos: Long?, date: Timestamp, dateKey: String, createdBy, createdByName, enteredAt: Timestamp, serverWrittenAt: Timestamp, deletedAt: Timestamp?, deletedBy: String?)`.

All fields use `com.google.firebase.Timestamp` for date types, `String` for ids, `Boolean` for `isActive`, `Double` for `quantityTons`, `Long?` for `pricePerTonCentavos`.

## Acceptance criteria

- [ ] `./gradlew assembleDebug` passes.
- [ ] Max's interfaces compile against these classes (no "unresolved reference" errors).
- [ ] Fields are in the same order as documented in `04-modelo-de-datos` (helps reading diffs).

## Pitfalls / notes

- **`val`, not `var`** — these are immutable. Mutations happen via `.copy(...)`.
- **`pricePerTonCentavos: Long?`** is nullable; everything else with `?` in the schema is nullable.
- **Don't add Firestore annotations** (`@DocumentId`, `@ServerTimestamp`) here — Max's real repo impl will handle the serialization mapping. Models stay clean.
- **Don't add methods** (no `isUnregistered()` helper, no `formatPrice()`) — keep the data classes pure. Helpers go in `data/util/`.
