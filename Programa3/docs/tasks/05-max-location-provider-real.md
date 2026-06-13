# Task 05 — Max — Real LocationProvider + permission UX

**Owner:** Max
**Estimated effort:** ~2 h
**Prerequisites:** 02; 00 not required (no Firebase involved)
**Day:** 2 (Jun 12)
**Blocks:** the Jun 12 trigger

## Goal

`FusedLocationProvider`-backed `LocationProvider` implementing the Q5
policy: single-shot high-accuracy fix, fine-or-nothing for reporting,
read-only app on denial.

## Inputs

- `docs/CONTEXT.md` — "Location privacy stance"
- `docs/grilling-session-2026-06-11.md` Q5

## Outputs

- `data/location/FusedLocationProvider.kt` —
  `getCurrentLocation(PRIORITY_HIGH_ACCURACY)` via
  `kotlinx-coroutines-play-services` `.await()`, ~15 s timeout →
  `Result.failure(FixTimeout)`. Map fine-grant-missing →
  `Result.failure(SinPermiso)`. Never `lastLocation` as primary (stale
  fixes lie about accuracy); acceptable as fallback **only** if fresher
  than 30 s.
- Permission request flow in Reportar entry point (FAB click):
  `rememberLauncherForActivityResult(RequestMultiplePermissions)` for
  FINE+COARSE; if only COARSE granted → same rationale path as denial
  ("se necesita ubicación precisa para reportar").
- Read-only gating: **only the FAB gates on location** — map,
  heatmap, recientes, detail never ask (zero-permission read path).
- Flip the Hilt binding: `FakeLocationProvider` → real (fake stays for
  ViewModel unit tests).

## Acceptance criteria

- [ ] Deny everything → app fully usable read-only; FAB explains.
- [ ] Coarse-only → treated as denial for reporting (rationale, link
      to settings).
- [ ] Fine granted → fix arrives with plausible `accuracyMeters`;
      airplane-mode/GPS-off → timeout failure surfaces as fixError
      with Reintentar (not a crash).
- [ ] No `ACCESS_BACKGROUND_LOCATION` anywhere; no continuous updates
      (verify: no `requestLocationUpdates` call in the codebase).

## Pitfalls / notes

- Emulator: set a location in extended controls or the fix suspends
  forever — that's what the timeout is for. Melanie's street run
  (task 16) is the honest test.
- `getCurrentLocation` can return null on success — map to failure,
  don't NPE.
- Don't cache fixes across reports: every Reporte gets a fix from
  *this* visit (CONTEXT.md: live capture).
