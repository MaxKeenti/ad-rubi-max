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
