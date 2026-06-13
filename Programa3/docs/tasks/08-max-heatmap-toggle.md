# Task 08 — Max — Severity-weighted heatmap toggle

**Owner:** Max
**Estimated effort:** ~1.5 h
**Prerequisites:** 07
**Day:** 3 (Jun 13)

## Goal

The "zonas con mayor incidencia" view (grilling Q10): a chip on the
map flips markers ↔ heatmap; heatmap intensity weighted by severity so
it answers "where are the *dangerous* zones."

## Inputs

- `docs/CONTEXT.md` — "Zonas con mayor incidencia"
- `android-maps-utils` `HeatmapTileProvider` + maps-compose `TileOverlay`

## Outputs

- `MapaViewModel`: `modoHeatmap: StateFlow<Boolean>` + toggle.
- `MapaScreen`: `FilterChip("Zonas")` over the map; when on, swap
  markers for a `TileOverlay` with `HeatmapTileProvider` built from
  `WeightedLatLng(latLng, reporte.severidad?.peso ?: Severidad.PESO_SIN_SEVERIDAD)`.
- Weights come from the `Severidad` enum (task 02) — single source.

## Acceptance criteria

- [ ] Chip flips views without resetting the camera.
- [ ] A severo cluster glows hotter than an equal-count leve cluster
      (verify with seeded fakes before real data).
- [ ] Provider data updates when the viewport reports change
      (`setWeightedData` + `clearTileCache`, not a new overlay per
      emission).
- [ ] Empty viewport → heatmap simply absent, no crash
      (HeatmapTileProvider throws on empty list — guard it).

## Pitfalls / notes

- `HeatmapTileProvider.Builder().weightedData(emptyList())` →
  `IllegalArgumentException`. Guard before building.
- Radius ~30–40 px reads well at city zoom; default 20 looks anemic.
- This task is protected in the contingency (nearly free, carries the
  heatmap story) — if time is tight elsewhere, cut 09/10 first.
