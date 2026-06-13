# Task 06 — Max — Real ReporteRepository (Firestore + Storage + geohash)

**Owner:** Max
**Estimated effort:** ~4 h
**Prerequisites:** 00, 02
**Day:** 2 (Jun 12)
**Blocks:** the Jun 12 trigger, 07–11

## Goal

The data centerpiece: upload-then-write report creation with geohash
denormalization (ADR-0002), viewport queries, recientes, confirmar
batch, soft-delete. Plus the real `SesionAnonima`.

## Inputs

- `docs/implementation_plan.md` §2 (frozen schema — do not drift)
- `docs/adr/0002-geohash-viewport-queries.md`
- `docs/grilling-session-2026-06-11.md` Q8, Q9, Q11, Q12, Q14
- Precedent: `Programa1/.../firestore/PurchaseRepositoryFirestoreImpl.kt`

## Outputs

- `data/auth/SesionAnonimaFirebase.kt` — `signInAnonymously()` once,
  uid StateFlow from `AuthStateListener`. Call `ensureSignedIn` at app
  start (read path needs it: rules require signed-in).
- `data/util/FotoCompressor.kt` — decode → scale to ≤1600 px long edge
  → JPEG q80 (~300–500 KB), **preserving EXIF datetime**.
- `data/repository/firestore/ReporteRepositoryFirestore.kt`:
  - `crearReporte`: pre-generate doc id (`collection.document().id`),
    compress, upload `reportes/{id}.jpg`, await `downloadUrl`, **only
    then** write the doc: lat/lng/geohash (GeoFireUtils)/accuracy/
    severidad?.valor/descripcion/fotoPath/fotoUrl/createdBy/
    confirmCount 0/`serverWrittenAt = FieldValue.serverTimestamp()`.
    Orphan photo on partial failure is the accepted artifact (Q8).
  - `observarViewport`: `GeoFireUtils.getGeoHashQueryBounds` →
    parallel `startAt/endAt` snapshot listeners → merge → **filter
    false positives with `GeoBounds.contains`** (extract as a pure
    function — unit-tested in task 13) → filter `deletedAt == null`.
    Zoom-out guard (ADR-0002): viewport diagonal > ~30 km → fall back
    to `recientes(200)` filtered client-side.
  - `recientes`: `whereEqualTo deletedAt null`? **No** — Firestore
    can't `whereEqualTo(null)` + orderBy cleanly; filter client-side
    like P1, `orderBy serverWrittenAt desc limit n`.
  - `confirmar`: `WriteBatch` = set `confirmaciones/{uid}` (
    `confirmedAt = serverTimestamp`) + `update(confirmCount,
    FieldValue.increment(1))`. `yaConfirmo`: doc-exists get.
  - `eliminar`: update exactly `deletedAt = serverTimestamp`,
    `deletedBy = uid` (client never checks 24 h alone — rules do too).
- Flip Hilt bindings: repository + sesión → real. **Fakes stay** for
  unit tests and the `:app:demoFake`…? No — single variant; fakes
  remain compiled but unbound (P1's commented-binding pattern).

## Acceptance criteria

- [ ] **Jun 12 trigger:** photo taken on device → doc in Firestore
      console with geohash + serverWrittenAt → marker on the real map.
- [ ] Kill app between upload and doc write (debugger) → orphan photo,
      no broken report rendered.
- [ ] confirmar twice from one install → second fails gracefully,
      count +1 only.
- [ ] UI modules import zero `com.google.firebase.*` (except model
      Timestamp) — `grep -r "com.google.firebase" app/src/main/java/com/example/bachewatch/ui/` is empty.

## Pitfalls / notes

- `severidad` stored as the lowercase string `valor`, absent when null
  — rules (task 11) validate the enum; don't store `"null"`.
- Geohash precision: use the query-bounds API, never manual prefix
  matching.
- Listener cleanup: `callbackFlow` + `awaitClose { registrations.forEach(remove) }`
  or the viewport flow leaks listeners on every pan.
- If the trigger fails by end of day: execute the contingency
  (README) — do not negotiate with it.
