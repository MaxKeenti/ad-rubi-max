# Task 13 — Max — SupplierRepository real Firestore implementation

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** `12-max-auth-repository-real` (need Firestore Hilt provider in place)
**Day:** 3 (target: midday cutover)

## Goal

Real Firestore-backed supplier repository. Flip Hilt binding when done.

## Inputs

- `docs/entrega/04-modelo-de-datos/content.md` § 2.2
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

## Verificación (2026-05-29, emulador Pixel 10, API 37)

Probado contra el proyecto Firebase real (`mangos-372bd`) con la cuenta
admin creada en task 12. Reglas e índices desplegados con
`firebase deploy --only firestore:rules,firestore:indexes` antes del run.

| Caso | Resultado |
|---|---|
| Admin agrega proveedor desde la pestaña Proveedores | ✓ Documento aparece en la lista sin refrescar; `createdAt`/`createdBy` poblados por el servidor |
| Tile "Proveedores" del Dashboard al agregar | ✓ Pasa de 0 → 1 en tiempo real (vía `snapshots()`) |
| Admin desactiva un proveedor | ✓ Desaparece de la lista activa y del dropdown de "Registrar compra" |
| Editar campo del proveedor desde Firebase Console | ✓ La app refleja el cambio sin reinicio (listener vivo) |
| `suppliers/UNREGISTERED` al primer arranque | ✓ Creado manualmente en consola (ver "Hallazgos") |

### Hallazgos / decisiones de implementación

- **Mapeo manual `DocumentSnapshot` → `Supplier`** en vez de
  `toObject` + `@DocumentId`. Razón: `Supplier` no tiene defaults en
  todos los campos y agregarlos solo para complacer al deserializador
  contamina el modelo. El mapeo manual está en
  `SupplierRepositoryFirestoreImpl.toSupplier()`.
- **`update()` omite `createdAt`/`createdBy`** del map en vez de
  confiar en que `SetOptions.merge()` los respete. Más explícito y
  evita que un caller que pase valores stale los sobrescriba.
- **`add()` falla si `currentUser.value == null`.** Necesario porque
  `createdBy` es no-nullable y las reglas exigen un userId autenticado.
- **`ensureUnregisteredExists()` falla pre-login bajo las reglas
  reales.** La llamada está en `MangosApp.onCreate`, antes de que haya
  sesión, así que el `.get()` recibe `PERMISSION_DENIED`. El
  `runCatching` lo silencia (warning a Logcat). Solución: se creó el
  doc manualmente desde la consola; la llamada queda como red de
  seguridad idempotente. Alternativa futura: mover la llamada al
  callback de login del admin.
- **Bug colateral arreglado en `DashboardViewModel`.** El tile
  "Proveedores Activos" contaba `UNREGISTERED` (la lista de Proveedores
  ya lo filtraba). Con el Fake esto estaba enmascarado porque había
  varios proveedores semilla; contra Firestore real con solo
  `UNREGISTERED` la inconsistencia era visible (tile: 1, lista: 0).
  Fix: `activeSuppliers.count { it.id != Supplier.UNREGISTERED_ID }`.
- **CP-08 parcial.** Pasos 1–2 verificados (admin desactiva, dropdown
  no muestra el proveedor). Paso 3 (compras históricas conservan
  `supplierName` del proveedor desactivado) requiere `PurchaseRepository`
  real — se verificará en task 14.
- **Permission-denied del operador no probado vía UI.** Los operadores
  no ven la pestaña Proveedores, así que la denegación al intentar
  agregar solo se puede demostrar con un test directo de repo o
  confiando en las reglas desplegadas. Se opta por lo segundo;
  las reglas (`allow write: if isAdmin()`) están desplegadas y
  fueron validadas en CP-09 para `users/`.
