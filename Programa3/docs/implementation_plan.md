# BacheWatch — Implementation Plan

**Decided:** 2026-06-11 (grilling session — see
`grilling-session-2026-06-11.md` for the full reasoning behind every
choice here)
**Team:** Max (all development + automated tests), Melanie (manual test
runbook, screenshots, release-gate suite runs)
**Budget:** June 11–15, full-time (~8–12 h/day → ~36–44 h available
against a ~31–39 h costed scope)
**Stack:** Kotlin, Jetpack Compose, Hilt, Firebase (Anonymous Auth,
Firestore, Storage), Google Maps SDK (`maps-compose`,
`android-maps-utils`), Coil, `geofire-android-common`. minSdk 26,
mirroring Programa2's scaffold.

---

## 1. Scope

### In scope (v1)

- One-screen report flow: system-camera photo → live GPS fix (soft
  accuracy gate, live indicator, retry) → optional severity chips +
  description → compress → upload-then-write.
- Map home: severity-colored tappable markers, severity-**weighted**
  heatmap toggle, geohash viewport queries (ADR-0002).
- Recientes list (`orderBy serverWrittenAt desc, limit 50`).
- Detail bottom sheet (single convergence point for map + list): photo,
  severity, description, relative date, confirm count, Confirmar button,
  Eliminar (own & <24 h).
- Confirmar: uid-keyed subcollection + batched `confirmCount` increment.
- Anonymous auth (ADR-0001); read-only app without location permission.
- Server-side rules (Firestore + Storage) + emulator test suite.
- Distribution via Firebase App Distribution + signed APK in the entrega.

### Explicitly out of scope (named v2 items)

- Offline reporting (online-required; in-place retry only — Q7).
- Edit flows; un-confirm; "Mis reportes";
  in-app moderation; pin-adjust; thumbnails (Resize extension);
  server-side clustering; durable accounts (anonymous-link upgrade).

### Explicit assumptions (rubric: suposiciones implícitas)

1. Urban context with mobile data at the curb; reporting requires
   connectivity; a failed report has a natural retry.
2. Single-shot GPS at capture; no tracking, no background location.
3. No authority actor exists; nobody marks potholes "reparado";
   moderation is manual via Firebase Console.
4. GPS drift (±5–15 m urban) is acceptable noise for density mapping;
   `accuracyMeters` is stored so this assumption is checkable later.

---

## 2. Data model (updated 2026-06-15)

```text
reportes/{reporteId}
  tipo: String               # "bache" | "otro"; absent legacy = bache
  lat, lng: Double           # machine-measured at capture, immutable
  geohash: String            # ADR-0002; written once
  accuracyMeters: Double     # soft gate: flagged, not rejected
  severidad: String?         # "leve" | "moderado" | "severo"
  descripcion: String?       # ≤200 chars
  fotoPath: String           # "reportes/{reporteId}.jpg"
  fotoUrl: String            # resolved downloadUrl, for Coil
  createdBy: String          # anonymous uid
  confirmCount: Long         # starts 0; batched increment only
  serverWrittenAt: Timestamp # the only clock (see CONTEXT.md)
  deletedAt: Timestamp?      # P1 soft-delete pattern
  deletedBy: String?

reportes/{reporteId}/confirmaciones/{uid}
  confirmedAt: Timestamp     # server timestamp; immutable; never deleted
```

Storage: `reportes/{reporteId}.jpg`, ≤1600 px long edge, JPEG q≈80.
Write ordering: upload photo → on success write doc (orphan photo is the
only partial-failure artifact).

## 3. Rules summary

- `reportes` create: signed-in, `createdBy == auth.uid`, shape-validated.
- `reportes` update: (a) creator setting exactly `deletedAt`/`deletedBy`
  within 24 h of `serverWrittenAt`, or (b) exactly
  `confirmCount == old + 1` (confirmar batch). Nothing else. No client
  delete.
- `confirmaciones` create: doc ID == `auth.uid`. No update/delete.
- Storage write: signed-in, `image/jpeg`, size cap (~1 MB).
- Accepted residual gap: count increment without confirmación doc
  (`getAfter()` hardening documented, deferred).

## 4. Architecture

MVVM Compose + Hilt; contract seam of three interfaces with in-memory
fakes (seeded with CDMX potholes — doubles as emulator demo data). The
seam exists for **testability**, not parallel dev (Q15):

- `ReporteRepository` — `crearReporte(foto, fix, severidad?, descripcion?)`,
  `observarViewport(bounds): Flow<List<Reporte>>`, `recientes(limit)`,
  `confirmar(reporteId)`, `eliminar(reporteId)`. Compression, Storage,
  geohash plumbing all hidden inside; UI never sees Firebase types.
- `LocationProvider` — `suspend fun fixActual(): LocationFix`.
- `SesionAnonima` — ensures sign-in, exposes `uid`.

Screens: **Mapa** (home, FAB, heatmap chip) · **Reportar** ·
**Recientes** · shared **detail bottom sheet**.

## 5. Schedule

| Day | Work | Checkpoint |
|---|---|---|
| Jun 11 | Bootstrap from P2 scaffold; Firebase project; Hilt; interfaces + fakes; anonymous auth; Reportar screen started | App runs against fakes |
| Jun 12 | Reportar complete (camera, compression, fix UI, upload-then-write); geohash repo; map markers live | **TRIGGER:** report travels camera → Firestore → real map marker end-to-end, or cut confirmar + delete-own now |
| Jun 13 | Heatmap toggle; detail sheet; confirmar; delete-own; Recientes; Firestore + Storage rules | Feature-complete |
| Jun 14 | Rules emulator suite; unit tests; keystore + App Distribution + release SHA-1 in Maps key; draft guía de pruebas → Melanie executes on-street | Distributed build in Melanie's hands |
| Jun 15 | Buffer; Melanie screenshots + runbook results; entrega polish; delivery | Ship |

### Contingency (decided in advance, executed mechanically)

If the Jun 12 trigger fails: cut **confirmar** (−3 h; schema keeps
`confirmCount` so it returns cheaply) and **delete-own** (−2.5 h;
moderation collapses to console backstop). Next rung: Recientes (−2 h).
Never cut: geo, maps, storage, camera, rules suite — they are the rubric.

## 6. Testing (Q16 split)

- **Max, with the code:** rules emulator suite
  (`firebase emulators:exec`, P1 pattern) covering confirm-as-other-uid,
  increment-by-2, delete-after-24h, delete-other's, shape violations;
  unit tests for viewport false-positive filtering and ViewModel states
  against fakes.
- **Melanie:** `guia-pruebas` runbook on a real phone on a real street —
  GPS accuracy behavior, camera handoff, mobile-data submit, permission-
  denied read-only path, confirm/delete flows; delivery screenshots;
  release-gate re-run of the automated suites.

## 7. Distribution (Q17)

Firebase App Distribution (Melanie is tester #1 — the distribution
channel *is* the test provisioning), signed APK attached to the entrega
as grader fallback. Traps pre-written into tasks: keystore stays out of
git (documented like `local.properties`); **release SHA-1 must be added
to the Maps API key restriction** or the distributed build renders a
blank map.

## 8. Entrega format

Spanish delivery documents are **HTML, mirroring Programa2's
implementation** — not Typst (Programa1's approach). Rationale: easier
to navigate and share on mobile devices on the go. The working docs in
`docs/` stay as the canonical sources; the entrega HTML is derived from
them at the end (Jun 14–15), reusing P2's styling/structure.
