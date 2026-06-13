# ADR-0002 — Geohash bounding-box queries for the map read path

**Status:** Accepted (grilling session, 2026-06-11, Q9)
**Deciders:** Max González

## Context

The map must load "reports within the visible region." Firestore cannot
express this natively: range filters on two fields (`lat` AND `lng`)
in one query are not allowed. Every Firestore geo app picks a strategy,
and this choice is the project's answer to the rubric's *arquitectura
escalable* line.

## Options considered

1. **Fetch-all** — query the whole `reportes` collection (`limit(500)`,
   newest first), filter/render client-side. Trivial; works at demo scale;
   and is the textbook non-scalable architecture — read cost grows with
   the dataset, not with what the user is looking at. At 10k reports every
   map open downloads 10k docs (or silently truncates).
2. **Geohash bounding-box queries** — denormalize a `geohash` string onto
   every report at write time (`GeoFireUtils.getGeoHashForLocation`). To
   load a viewport, compute its geohash query bounds
   (`getGeoHashQueryBounds`, typically 5–9 range queries), run them, and
   filter false positives client-side (geohash cells are rectangles that
   overhang the requested radius/bounds). Firebase's own documented
   answer to geo-queries.
3. **Server-side aggregation** — Cloud Function maintaining pre-clustered
   zone tiles. The real answer at city scale; over-engineering with
   nothing on the map yet, and it would be the only thing in Programa3
   requiring Cloud Functions.

## Decision

**Option 2: geohash queries**, behind `ReporteRepository.observarViewport
(bounds)` so the UI only ever sees `Flow<List<Reporte>>`.

Schema consequence: every Reporte carries `lat: Double`, `lng: Double`,
`geohash: String` — written once at capture, immutable (reports don't
move). This is the same denormalize-at-write-time move as Programa1's
`dateKey`: decide the bucket once, at write, so reads are cheap string
range matches.

Guards:
- **Zoom-out guard:** below a zoom threshold (city-wide), don't run
  unbounded geo-queries; serve the heatmap from the most recent N reports
  instead.
- False-positive filtering after the bound queries is mandatory, not
  optional — geohash bounds over-cover.

## Consequences

- Read cost scales with the **viewport**, not the dataset — the
  one-paragraph scalability justification: panning a neighborhood reads
  that neighborhood, whether the city has 200 reports or 200,000.
- Costs: ~half a day including false-positive filtering; 5–9 parallel
  range queries per viewport change (debounce camera-idle events); the
  `geofire-android-common` dependency.
- Soft-deleted rows are filtered with `deletedAt == null` alongside the
  geohash range — requires a composite index, captured in
  `firestore.indexes.json`.
- **Documented v2 path:** when a viewport routinely exceeds N results,
  move to server-side clustering/tiles (option 3). The write-side schema
  already supports it; nothing rearchitects.
