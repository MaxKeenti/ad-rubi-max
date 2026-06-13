# Task 09 — Max — Detail bottom sheet + Confirmar + Eliminar

**Owner:** Max
**Estimated effort:** ~3.5 h
**Prerequisites:** 03, 06, 07
**Day:** 3 (Jun 13)
**Contingency:** if the Jun 12 trigger failed, this task shrinks to a
read-only sheet (photo/severity/description/date) — cut Confirmar and
Eliminar, keep the sheet.

## Goal

The single convergence point (grilling Q13): map markers and Recientes
rows open the **same** `ModalBottomSheet` with photo, severity,
descripción, relative date, confirm count, Confirmar, and Eliminar
(own & <24 h).

## Inputs

- `docs/CONTEXT.md` — Confirmación semantics, soft-delete & moderation
- `docs/grilling-session-2026-06-11.md` Q11, Q12

## Outputs

- `ui/detalle/DetalleReporteSheet.kt` + `DetalleViewModel` — takes a
  reporte id + the already-loaded `Reporte` (no refetch for display;
  `yaConfirmo` is the only extra read).
- Confirmar button states: enabled → loading → confirmed-disabled
  ("Confirmado ✓"); count updates optimistically, reverts on failure.
  **No un-confirm** — a confirmación is a historical fact.
- Eliminar: visible only when `createdBy == uid` **and**
  `serverWrittenAt` within 24 h; confirmation dialog
  ("Se eliminará tu reporte. Las fotos borrosas se reportan de nuevo,
  no se editan."); calls `eliminar`, dismisses sheet, reporte vanishes
  from map/list (queries filter `deletedAt`).
- Wire openers: marker tap (07) and Recientes row tap (03).

## Acceptance criteria

- [ ] Same sheet component from both entry points (one file, no copies).
- [ ] Confirm from install A on install B's reporte → count +1, button
      locks; reopening the sheet shows it still locked (`yaConfirmo`).
- [ ] Eliminar absent on others' reportes and on own >24 h (seed data
      covers both cases).
- [ ] Sheet handles a reporte deleted underneath it (snapshot update →
      dismiss gracefully).

## Pitfalls / notes

- There is **no edit affordance anywhere** — delete-and-retake is the
  documented stance; don't "helpfully" add one.
- Relative date: reuse `TiempoRelativo` (task 03).
- Optimistic confirm: increment local count immediately; Firestore
  latency on mobile data would otherwise make the button feel dead —
  but reconcile with the snapshot, don't double-count.
