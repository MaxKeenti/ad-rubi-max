# Task 07 — Max — Map home with live viewport markers

**Owner:** Max
**Estimated effort:** ~3 h
**Prerequisites:** 03; 06 for real data (works against fakes first)
**Day:** 2 (Jun 12)
**Blocks:** 08, 09

## Goal

Replace the Mapa placeholder with `maps-compose`: severity-colored
tappable markers driven by `observarViewport`, camera-follows-viewport
queries.

## Inputs

- `docs/adr/0002-geohash-viewport-queries.md`
- `docs/grilling-session-2026-06-11.md` Q10, Q13
- maps-compose docs (CameraPositionState, MarkerState)

## Outputs

- `ui/mapa/MapaScreen.kt` — `GoogleMap` + `CameraPositionState`
  (initial: CDMX centro, zoom ~12). On camera **idle** (not on every
  frame), convert `Projection.visibleRegion.latLngBounds` → `GeoBounds`
  → `viewModel.onViewportChanged`. Markers from state: hue by
  severidad (verde/ámbar/rojo, gris for null), tap → select reporte
  (detail sheet lands in task 09; until then a snackbar with the id).
- My-location layer enabled only if permission already granted (never
  prompts — only the FAB prompts, task 05).
- Loading/empty states: subtle progress while first snapshot resolves.

## Acceptance criteria

- [ ] Markers appear/disappear as you pan/zoom across CDMX; no query
      storm while the camera is moving (idle-debounced).
- [ ] Severity colors match the Recientes chips.
- [ ] Map state survives navigation to Recientes and back (camera not
      reset).
- [ ] Without location permission the map still renders tiles +
      markers (read-only path).

## Pitfalls / notes

- **API key**: debug SHA-1 must be on the key (task 00) or you get
  blank beige tiles and a logcat error — check logcat *first* when
  the map looks dead.
- `flatMapLatest` on viewport changes cancels the previous listeners —
  that's the intended lifecycle, don't accumulate.
- Marker recomposition: key markers by reporte id
  (`Marker(state = rememberUpdatedMarkerState(...), tag = id)`), or
  pan-jank.
- Keep `GeoBounds` ↔ `LatLngBounds` conversion in the UI layer — the
  repository must stay Maps-SDK-free (unit-testability).
