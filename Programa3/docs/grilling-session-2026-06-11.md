# Grilling Session — 2026-06-11

Domain & architecture grilling session over the BacheWatch brief
(`#GeoLocalización #Mapas #FireStorage #Cámara`). Worked through 18
decision branches; produced this log, `CONTEXT.md`, two ADRs, and
`implementation_plan.md`. Unlike Programa1, there was no pre-existing
plan to rewrite — the design was built from the brief during the session.

**Participants:** Max González, Claude (Fable 5)
**Starting point:** BacheWatch course brief (pothole reporting app)
**Deadline context:** delivery June 15; full-time solo development
**Outputs:**
- `docs/CONTEXT.md` — domain glossary
- `docs/adr/0001-anonymous-auth.md`
- `docs/adr/0002-geohash-viewport-queries.md`
- `docs/implementation_plan.md`
- `docs/work-division.md`
- This log

---

## Q1 — What is a "Reporte," actually?

**Options:** (A) observation/sighting, immutable, duplicates welcome;
(B) incident/case with lifecycle (`reportado → reparado`) requiring dedup
and an authority actor; (C) hybrid clustering.

**Decision:** (A) Observation, **plus a lightweight "confirmar"** action
("sigue ahí") on existing reports.

**Why it mattered:** Duplicate density *is* the incidence signal, so dedup
is not a bug to fix. No status lifecycle, no municipality actor, no merge
flow. Confirmar gives a convergence/freshness signal at a fraction of
dedup's cost. Everything downstream (immutability, no edits, heatmap
semantics) flows from "Reporte = observation."

---

## Q2 — Identity model

**Options:** (i) Firebase Anonymous Auth; (ii) email/password accounts;
(iii) Google Sign-In; (iv) no auth.

**Decision:** (i) Anonymous Auth. Captured in **ADR-0001**.

**Why it mattered:** Only option aligned with the brief's "reportar de
manera sencilla" while still giving every write an `auth.uid` for
security rules and one-confirm-per-user. Trade-off accepted: identity
dies with the installation; abuse control is per-install, not per-person.
Forward-compatible: anonymous accounts can be upgraded without losing uid.

---

## Q3 — Where is the citizen when the report is created?

**Options:** (α) at the pothole, live capture + live GPS fix;
(β) report later from anywhere (gallery + pin-drop); (γ) hybrid with
pin-adjust.

**Decision:** (α). Hybrid noted as tempting; kept reversible by storing
`accuracyMeters` on every report so a pin-adjust step can be justified
later with data.

**Why it mattered:** Strongest data-quality guarantee ("every coordinate
was machine-measured at the pothole"), one-screen report flow, no gallery,
no `READ_MEDIA_IMAGES`. "Remembered potholes" explicitly out of scope.

---

## Q4 — Camera: CameraX or system intent?

**Decision:** System camera intent (`ActivityResultContracts.TakePicture`).
No `CAMERA` permission needed; rock-solid across devices. The 5–15 s gap
between shutter and GPS fix is irrelevant for a stationary user. Flow
note: warm up the GPS fix while the user is in the camera.

---

## Q5 — Location policy (bundle)

- **(a)** Request `ACCESS_FINE_LOCATION`; a coarse-only grant is treated
  as denial *for reporting*. Single-shot
  `getCurrentLocation(PRIORITY_HIGH_ACCURACY)`; never continuous
  tracking, never background location.
- **(b)** Permission-denied users get a fully functional **read-only**
  app; only the "Reportar" button gates on location.
- **(c)** **Soft accuracy gate** — live accuracy indicator with retry,
  but save is always allowed; poor fixes are flagged data
  (`accuracyMeters`), not rejected data.

---

## Q6 — Report contents

**Decision:** Required: photo + coordinates. Optional & nullable:
`severidad` (one-tap chips: leve/moderado/severo, no default) and
`descripcion` (≤200 chars). **Category cut** from v1; "otros desperfectos"
out of scope, schema forward-compatible via a future `category` field
(absent = bache).

**Why it mattered:** Severity is the highest-value single tap — it turns
"dots on a map" into "dangerous zones are red" via heatmap weighting (Q10).

---

## Q7 — Offline reporting?

**Options:** (P) full WorkManager offline queue; (Q) online-required with
in-place retry; (R) persistent draft, manual retry.

**Decision:** (Q), documented as an explicit assumption.

**Why it mattered:** A report is a two-phase write (Storage upload +
Firestore doc); Storage uploads do not queue offline for free the way
Firestore writes do, so offline here costs ~1 day plus partial-failure
bugs. Target context is urban streets with mobile data; a failed report
has a natural retry (the pothole isn't going anywhere). Inverse of
Programa1's dock situation — paying for that robustness here would be
scope dishonesty.

---

## Q8 — Photo pipeline (bundle)

- Compress client-side: max 1600 px long edge, JPEG q≈80 → ~300–500 KB.
- Single image, **no thumbnails** in v1; Firebase Resize Images extension
  named as the config-only growth path.
- Client pre-generates the Firestore doc ID, uploads to
  `reportes/{reporteId}.jpg`, and **only on upload success** writes the
  Firestore doc (`fotoPath` + resolved `fotoUrl`). Partial failure leaves
  a harmless orphaned photo, never a broken report.

---

## Q9 — Geo-query strategy

**Options:** (A) fetch-all + client filter; (B) geohash bounding-box
queries (geofire-android); (C) server-side pre-clustered tiles.

**Decision:** (B). Captured in **ADR-0002**.

**Why it mattered:** This is the project's scalable-architecture
centerpiece: reads scale with the viewport, not the dataset. Same
denormalize-at-write-time move as Programa1's `dateKey` — decide the
bucket once, at write. Schema: `lat`, `lng`, `geohash`, written once,
immutable. Zoom-out guard: city-wide views are served from the most
recent N reports instead of unbounded geo-queries. Documented v2:
server-side clustering when a viewport exceeds N results.

---

## Q10 — Map provider & zones rendering (bundle)

- **Google Maps SDK** + `maps-compose` + `android-maps-utils`. Same Google
  Cloud project as Firebase; native Android map loads are not billed. API
  key restricted to package name + SHA-1 (debug **and release**).
- **Markers + heatmap toggle**: default view is tappable severity-colored
  markers; a chip flips on `HeatmapTileProvider`. Heatmap points are
  **weighted by severity** (leve 1, moderado 2, severo 3, unset 1.5), so
  the zones view answers "where are the *dangerous* zones."

---

## Q11 — Confirmar mechanics (bundle)

- Subcollection `reportes/{reporteId}/confirmaciones/{uid}` — the uid
  **is** the doc ID, making one-confirm-per-user a structural guarantee
  (same trick as Programa1's `dateKey`: encode the invariant in the key).
- Denormalized `confirmCount` on the report, updated in the same
  `WriteBatch` via `FieldValue.increment(1)`. Rules allow report updates
  only when the diff touches exactly `confirmCount` with new = old + 1.
  Known residual gap (increment without creating the confirmación doc)
  documented; `getAfter()` batch introspection named as the hardening.
- **No un-confirm** — a confirmación is a historical fact.

---

## Q12 — Mistakes & abuse (bundle)

- **No edits, ever.** The payload is machine-captured; delete-and-retake
  beats an edit screen.
- **Creator soft-delete within 24 h of `serverWrittenAt`** — Programa1's
  rule pattern reused verbatim (`deletedAt`/`deletedBy`, queries filter
  `deletedAt == null`, photo stays in Storage, hard-delete never).
- **No in-app moderation.** Firebase Console is the backstop; documented
  as an explicit assumption with the v2 path named (moderation queue +
  abuse-report flow).

---

## Q13 — Screen map

**Decision:** Map-first + Recientes. Map is home; FAB → Reportar (one
screen); top-bar action → Recientes (newest-first list, `limit 50`).
Markers and list rows both open the **same detail bottom sheet** (photo,
severity, description, relative date, confirm count, Confirmar button,
Eliminar if own & <24 h). "Mis reportes" tab cut — its only job is
already in the detail sheet.

---

## Q14 — Timestamps & schema freeze

**Decision:** Single `serverWrittenAt` (`FieldValue.serverTimestamp()`).

**Why it mattered:** Programa1 needed three timestamps because of
back-dating and offline queueing. Q3 (live capture) killed back-dating;
Q7 (online-required) killed the client/server gap. Both forces removed
by design, so the model collapses to one clock. `dateKey` also dropped —
no day-bucket queries in scope. Photo EXIF preserves capture time as a
free forensic backstop. Full schema in `implementation_plan.md`.

---

## Q15 — Architecture

**Decision:** Programa1/2 machinery, minimal seam: MVVM Compose + Hilt,
three interfaces — `ReporteRepository` (whole domain: create/observe
viewport/recientes/confirmar/eliminar), `LocationProvider`,
`SesionAnonima` — with in-memory fakes.

**Purpose shift:** with Max running all development, the seam is no
longer a parallel-work coordination device (P1); it survives as the
**testability** device — fakes make ViewModels unit-testable and give
Melanie a deterministic Firebase-free app. Geohash plumbing hides behind
`observarViewport(bounds): Flow<List<Reporte>>`; UI never sees
GeoFireUtils or Firebase types.

---

## Q16 — Test strategy

**Decision:** Split by coupling. **Max** writes the automated layer
alongside the code: rules emulator suite (non-negotiable — the
exactly-plus-one and 24 h-window rules are wrong until tested), viewport
false-positive filtering, ViewModel states. **Melanie** owns the manual
layer: a `guia-pruebas` runbook executed on a real phone on a real street
(the only honest test of GPS/camera/mobile-data paths), delivery
screenshots, and release-gate runs of the automated suites.

---

## Q17 — Distribution

**Decision:** **Firebase App Distribution** as the channel (Melanie
receives the build through it and tests against it — one artifact, two
rubric lines), plus a signed APK attached to the entrega as fallback.
Implementation traps written into tasks: real keystore kept out of git;
**release SHA-1 added to the Maps API key restriction** or maps render
blank in exactly the distributed build.

---

## Q18 — Budget & scope honesty

**Inputs:** deadline June 15; full-time, 8–12 h/day → ~36–44 h available.
Costed scope: ~31–39 h.

**Decision:** Full scope, no pre-cuts, with a **contingency trigger**
instead of a ladder-in-waiting: if by end of June 12 a report cannot
travel camera → Firestore → real map marker end-to-end, cut confirmar
(−3 h) and delete-own (−2.5 h) immediately. Further rungs if needed:
Recientes list (−2 h). Severity weighting is protected — nearly free and
carries the heatmap story. Geo + maps + storage + camera are untouchable
(they are the rubric).

---

## Themes & meta-observations

- **"Reporte = observation" was the load-bearing frame.** Immutability,
  no-dedup, no-edits, single-timestamp, and heatmap-as-density all became
  forced moves once Q1 landed.
- **Q3 + Q7 actively *deleted* complexity Programa1 had to pay for.**
  Live capture killed back-dating; online-required killed the offline
  queue and the three-timestamp model. Knowing which forces are absent
  is as valuable as knowing which are present.
- **The geohash decision (Q9) is the project's architectural
  centerpiece** — it's the honest answer to the rubric's "arquitectura
  escalable" line, with a one-paragraph justification and a named v2.
- **Patterns were reused across projects deliberately:** the 24 h
  soft-delete window, encode-the-invariant-in-the-key, fakes-behind-
  interfaces, optional-fields-never-block-save. Citing precedent is part
  of the design justification.
- **Two ADRs is again the right count.** Anonymous auth and geohash
  queries are the two hard-to-reverse, surprising-without-context
  decisions. Everything else is glossary (CONTEXT.md) or plan.
