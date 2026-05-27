# Task 13 — Max — SupplierRepository real Firestore implementation

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** `12-max-auth-repository-real` (need Firestore Hilt provider in place)
**Day:** 3 (target: midday cutover)

## Goal

Real Firestore-backed supplier repository. Flip Hilt binding when done.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.2
- `data/repository/SupplierRepository.kt` (interface)
- `data/model/Supplier.kt`

## Outputs

- `data/repository/firestore/SupplierRepositoryFirestoreImpl.kt`:
  - `observeActive()` — `firestore.collection("suppliers").whereEqualTo("isActive", true).snapshots().map { ... }`
  - `observeAll()` — same without the filter (callers — only admins per UI — get UNREGISTERED too; the UI filters it visually).
  - `getById(id)` — single document fetch.
  - `add(supplier)` — generate id via `.document().id` or let Firestore assign; populate `createdAt = serverTimestamp()`, `createdBy = currentUser.id` (the impl needs an `AuthRepository` injection).
  - `update(supplier)` — `.set(supplier)` with merge. Don't touch `createdAt`/`createdBy`.
  - `deactivate(id)` — `.update("isActive", false)`.
  - `ensureUnregisteredExists()` — read `suppliers/UNREGISTERED`; if 404, create with name "Proveedor no registrado", `isActive = true`. Idempotent. Called from `MangosApp.onCreate` or first-launch.

- **Modify `data/di/AppModule.kt`:** flip `@Binds` to the new impl.

## Acceptance criteria

- [ ] CP-08 (supplier deactivation visibility) passes against real Firestore.
- [ ] Adding a supplier as admin works and appears in real-time.
- [ ] Adding a supplier as operator **fails** with a permission error (rules from task 15 enforce this — verify after task 15 lands).
- [ ] `ensureUnregisteredExists()` runs at app start and creates the doc if missing.

## Pitfalls / notes

- **Document → data class mapping:** use Firestore's `documentSnapshot.toObject(Supplier::class.java)` with a no-arg constructor on `Supplier` data class. Kotlin data classes get this for free **if all fields have defaults**. Add `= ""`, `= 0L`, etc., defaults on every field.
- **Or** use a manual mapper — more code, fewer surprises. Your call.
- **The `id` field:** Firestore doesn't auto-populate it from doc id. Either use `@DocumentId` annotation (requires the toObject path) or manually `.copy(id = doc.id)` in the mapping.
- **`snapshots()` is the KTX flow function** — `implementation("com.google.firebase:firebase-firestore-ktx")` is already in your deps (BOM).
- **Tell Melanie when this lands.**
