# Task 14 — Max — PurchaseRepository real Firestore implementation

**Owner:** Max
**Estimated effort:** ~2.5 hours
**Prerequisites:** `13-max-supplier-repository-real`, `11-max-money-datekey-utils`
**Day:** 3–4 (cutover: end of day 3 or morning day 4)

## Goal

The hardest data-layer task. Real Firestore-backed purchase repository
with offline persistence, three-timestamp semantics, `dateKey`
computation on write, soft-delete on read filter, and the pending-sync
metadata bridge for the Dashboard's badge.

**If this needs to be promoted to `/gsd-plan-phase`** because complexity
exceeds a single agent's reach, do it here. It's the highest-risk task
in the project.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.3 (full schema, three timestamps, dateKey)
- `docs/Programa1/entrega/06-glosario/content.md` (Purchase, denormalization rules)
- `docs/Programa1/adr/0002-server-side-authz.md` (rules constrain what queries are valid)
- `data/repository/PurchaseRepository.kt` (interface)
- `data/util/DateKey.kt`, `MoneyFormatter.kt`

## Outputs

- `data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt`:
  - `observeByDateKey(key)` — `collection("purchases").whereEqualTo("dateKey", key).whereEqualTo("deletedAt", null).orderBy("enteredAt", DESC).snapshots()`.
  - `observeBySupplier(supplierId, limit)` — composite query with `deletedAt == null`, ordered by `date DESC`. Requires the index from task 16.
  - `observeRecent(limit)` — global recent, `deletedAt == null`, ordered `enteredAt DESC`, limit applied.
  - `getTodaySummary(dateKey)` — fetches today's purchases, computes `TodaySummary` in memory (small data set; no need for aggregation queries).
  - `add(purchase)` — sets `serverWrittenAt = FieldValue.serverTimestamp()`, `dateKey = purchase.date.toDateKey()` (recompute defensively), populates denormalized `supplierName` and `createdByName` by looking up the cached supplier and current user. Generates id with `.document().id`. Returns the new id.
  - `update(purchase)` — recomputes `dateKey` from `purchase.date` (in case the user changed it). Does NOT touch `serverWrittenAt`, `createdBy`, `enteredAt`, denormalized names.
  - `softDelete(id, deletedBy)` — `.update(mapOf("deletedAt" to FieldValue.serverTimestamp(), "deletedBy" to deletedBy))`.

- A `Flow<List<Pair<Purchase, Boolean>>>` variant or extension that pairs each Purchase with its `metadata.hasPendingWrites` flag — for the Dashboard badge. **Discuss with Melanie if this requires an interface change** (it likely does); if so, re-open the interface with a 5-min sync and update.

- **Modify `data/di/AppModule.kt`:** flip to the real impl.

## Acceptance criteria

- [ ] CP-02 (normal capture), CP-03 (no price), CP-04 (UNREGISTERED) all pass against real Firestore.
- [ ] CP-06 (soft-delete) passes — doc remains in Firestore with `deletedAt` set.
- [ ] CP-07 (offline capture) passes — purchase is captured offline, badge shows, syncs on reconnect.
- [ ] CP-11 (timezone of `dateKey`) passes — capture at 23:30 Mexico time produces the local-day dateKey.
- [ ] CP-05 (24h edit window) **client-side** check works; server-side enforcement comes from task 15.

## Pitfalls / notes

- **Persistent cache must be enabled** (you set it up in task 12). Without it, offline writes are lost on app restart.
- **`metadata.hasPendingWrites`** comes from `QuerySnapshot.metadata` — emit it alongside each Purchase. The cleanest API is `data class PendingAware(val purchase: Purchase, val isPending: Boolean)` and the Flow yields a `List<PendingAware>`.
- **Denormalized name fields are set at write only.** Don't fan-out on supplier rename — that's explicitly disallowed in the glossary.
- **`dateKey` is set client-side** before write, because `serverTimestamp()` resolves to null in the snapshot until the server roundtrip completes. The client knows the local date right away.
- **Don't use `whereEqualTo("deletedAt", null)`** if Firestore's null-handling bites you — alternative is to omit `deletedAt` entirely on live docs and check field-existence. Test both; the former is cleaner if it works.
- **Composite indexes** are created in task 16. If your queries fail with "this query requires an index" errors, that means task 16 isn't done yet — the Firebase Console gives you a one-click link to create the missing index, useful for dev.
- **Tell Melanie when this lands.** This is the cutover that will likely surface bugs in her screens. Plan to be available right after the merge for a debugging session.

## Verificación (2026-05-29)

`./gradlew assembleDebug` termina en BUILD SUCCESSFUL contra el árbol con
el binding flipeado en `AppModule` a
`PurchaseRepositoryFirestoreImpl`. La verificación contra Firestore real
queda condicionada al siguiente arranque conjunto con Melanie (cutover
acordado para mañana, día 4) — ADRs y reglas (task 15) deben estar
desplegadas antes de marcar CP-05/CP-06 como pasados de punta a punta.

### Hallazgos / decisiones de implementación

- **Cambio aditivo en la interfaz `PurchaseRepository`** para soportar el
  badge "pendiente" del Dashboard:
  - Nueva `data class PendingAware(val purchase: Purchase, val isPending: Boolean)`.
  - Nuevo método `observeRecentWithPending(limit): Flow<List<PendingAware>>`.
  - `observeRecent(limit)` se conserva con su firma original
    (`Flow<List<Purchase>>`) para no romper a `PurchaseHistoryViewModel`.
  - `FakePurchaseRepository` implementa la nueva firma derivándola de
    `observeRecent` con `isPending = false` (la pila offline solo es
    observable contra Firestore real).
- **`DashboardViewModel` migrado** al nuevo método. Es la única edición
  fuera de mi carril; justificada porque CP-07 exige el badge y la VM ya
  exponía el campo `isPendingSync` (Melanie lo dejó preparado en
  `DashboardPurchaseUi`). `DashboardScreen` ya pinta `PendingSyncBadge()`
  cuando el flag es true.
- **`metadata.hasPendingWrites()` por documento** dentro de la query:
  cada `DocumentSnapshot` del `QuerySnapshot` carga su propio
  `SnapshotMetadata`. Más fino que el flag agregado del `QuerySnapshot`,
  que prendería el badge para *todos* los recientes si hay un solo
  pendiente.
- **`serverWrittenAt` con fallback a `enteredAt`** en el mapper. Mientras
  la escritura sigue en cola offline, `FieldValue.serverTimestamp()` se
  resuelve como `null` en el snapshot local. La ventana de 24h del
  cliente (`AddEditPurchaseViewModel.canEdit`,
  `PurchaseHistoryViewModel.canEdit`) compara contra `serverWrittenAt`,
  así que sin el fallback una compra recién capturada offline se
  consideraría "ya no editable". Con el fallback, mientras está pendiente
  se compara contra `enteredAt` (también reciente); cuando el servidor
  resuelve el timestamp, el siguiente snapshot trae el valor canónico.
- **`add()` re-denormaliza nombres defensivamente.** Aunque
  `AddEditPurchaseViewModel` ya populariza `supplierName` y
  `createdByName`, el repo vuelve a leer
  `SupplierRepository.getById(supplierId)` y
  `authRepository.currentUser.value.displayName` y los usa si están
  disponibles, cayendo al valor del caller en caso contrario. Mantiene el
  trust boundary en la capa de datos.
- **`update()` no toca** `serverWrittenAt`, `createdBy`, `createdByName`,
  `supplierName` ni `enteredAt`. Solo recalcula `dateKey` desde
  `purchase.date` por si el operador cambió la fecha en una corrección.
  Por ADR-0002 nunca se debe sobrescribir `serverWrittenAt` — ahí vive el
  reloj de la ventana de 24h.
- **`whereEqualTo("deletedAt", null)`** funciona contra el shape de
  índices ya declarados (`dateKey ASC, deletedAt ASC, enteredAt DESC`,
  etc.) y `add()` siempre escribe el campo explícitamente como `null`
  para que la igualdad lo encuentre. El path alternativo (omitir el
  campo en vivos) queda como plan B si Firestore lo rechaza en
  producción.
- **`observeByDateRange` ordena en cliente.** Firestore exige que en
  consultas con range filter, el primer `orderBy` sea sobre el campo del
  range. Para no comprometer el sort por `enteredAt DESC` ni declarar un
  índice nuevo, el repo trae el bucket de días con
  `whereGreaterThanOrEqualTo` + `whereLessThan` sobre `dateKey` y luego
  `sortedByDescending { enteredAt.seconds }` en memoria. Conjuntos
  esperados (≤ 1 mes) caben sin problema.
- **CP-02, CP-03, CP-04, CP-06, CP-07, CP-11 trazadas en código** —
  pendientes verificación end-to-end contra emulador Pixel + Firestore
  real durante el cutover con Melanie. CP-05 lado-cliente trazado;
  enforcement servidor llega con task 15.
