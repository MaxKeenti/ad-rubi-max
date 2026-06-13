# Work Division — Max + Melanie

**Decided:** 2026-06-11 (grilling session, Q15–Q17)
**Team:** González Calzada Maximiliano (development), Sosa Montoya
Melanie Rubí (testing & delivery evidence)
**Budget:** June 11–15, Max full-time (~8–12 h/day)
**Branches:** `max`, `rubi` → merge into `main`

Unlike Programa1's contract-first *parallel development* split, Programa3
splits by **role**: Max runs all development; Melanie runs the tests.
The detailed schedule lives in `implementation_plan.md` §5.

---

## 1. Why the interfaces + fakes survive a solo-dev setup

The P1 machinery (repository interfaces, in-memory fakes, Hilt
Fake → Real flip) is kept, but its purpose changed: it is no longer a
coordination seam between two developers — it is the **testability
device**. Fakes make ViewModels unit-testable without Firebase, and give
Melanie a deterministic, seeded app (CDMX demo potholes) that runs on any
emulator with zero credentials. Anyone asking "why Hilt for one
developer" gets this paragraph.

## 2. Max — development + automated tests

Automated tests are written *with* the code, not handed off — test-after
by another person against moving code would thrash:

- All app code (see plan §4: `ReporteRepository`, `LocationProvider`,
  `SesionAnonima`, four screens).
- Firestore + Storage rules, indexes, seed data.
- **Rules emulator suite** (non-negotiable; P1 pattern,
  `firebase emulators:exec`): confirm-as-other-uid fails,
  `confirmCount` increment-by-2 fails, delete-after-24h fails,
  delete-someone-else's fails, shape violations fail.
- Unit tests: geohash viewport false-positive filtering, ViewModel
  states against fakes.
- Keystore, App Distribution setup, release SHA-1 → Maps key.
- Drafting the `guia-pruebas` runbook Melanie executes.

## 3. Melanie — manual testing + delivery evidence

The human goes where automation is blind: real GPS, real camera, real
mobile data.

- Execute the **guía de pruebas on a real phone on a real street**:
  report flow end-to-end, soft accuracy gate behavior (urban canyon vs
  open sky), camera handoff, mobile-data submit + airplane-mode retry
  path, permission-denied read-only mode, confirmar, delete-own,
  heatmap toggle. File findings against each step.
- Receive the build **through Firebase App Distribution** — her install
  is itself the distribution proof (one artifact, two rubric lines).
- Capture the entrega screenshots.
- Release gate: re-run Max's automated suites so a second person has
  reproduced them before delivery.

## 4. Entrega format

Delivery documents are **HTML, mirroring Programa2** (not Programa1's
Typst) — easier to navigate and share on mobile. Working docs in
`docs/` are canonical; the Spanish entrega HTML is derived from them on
Jun 14–15 reusing P2's structure/styling. Melanie's screenshots and
runbook results feed directly into it.
