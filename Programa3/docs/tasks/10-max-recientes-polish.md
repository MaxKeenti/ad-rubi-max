# Task 10 — Max — Recientes: wire to real data + polish

**Owner:** Max
**Estimated effort:** ~1 h
**Prerequisites:** 03, 06, 09
**Day:** 3 (Jun 13)
**Contingency:** second rung — cut entirely (−2 h) if Day 3 overruns;
the route and top-bar action are one-line removals.

## Goal

The Recientes list built in task 03 against fakes, now backed by the
real `recientes(50)` query, rows opening the detail sheet.

## Outputs

- Row tap → `DetalleReporteSheet` (same component as map).
- Empty state ("Aún no hay reportes") and offline-ish error state
  (Firestore listener error → message, not blank).
- Pull state: snapshot listeners are live — **no** swipe-to-refresh;
  it would be a placebo on a realtime listener.

## Acceptance criteria

- [ ] Newest report appears at the top within seconds of another
      device creating it (listener, not poll).
- [ ] Exactly ≤50 items; no pagination UI (out of scope, documented).
- [ ] Soft-deleted reportes vanish live.

## Pitfalls / notes

- Firestore index: `orderBy serverWrittenAt desc` on a single field
  needs no composite index — if the console asks for one, the query
  drifted from the plan; fix the query, don't add the index.
