# Task 04 — Max — Reportar screen (camera + fix + submit)

**Owner:** Max
**Estimated effort:** ~4 h
**Prerequisites:** 03 (started Day 1 with fakes; camera + FileProvider
finish Day 2)
**Day:** 1–2 (Jun 11–12)
**Blocks:** the Jun 12 trigger

## Goal

The one-screen report flow (grilling Q3–Q6): system-camera photo →
live GPS fix with soft accuracy gate → optional severity chips +
descripción → enviar, with in-place retry on failure (Q7).

## Inputs

- `docs/CONTEXT.md` — "Where and when it gets created", photo evidence,
  location privacy stance
- `docs/grilling-session-2026-06-11.md` Q4, Q5, Q7

## Outputs

- `ui/reportar/ReportarViewModel.kt` — UI state: foto, fix /
  buscandoFix / fixError, severidad (no default!), descripcion
  (truncate at 200), enviando / enviado / envioError. `enviar()` calls
  `crearReporte`; failure keeps everything on screen with a Reintentar
  button — nothing survives process death by design.
- `ui/reportar/ReportarScreen.kt` — photo slot, GPS card (spinner →
  "±N m" colored by accuracy, Reintentar fix), severity FilterChips,
  200-char counter field, Enviar (enabled = foto + fix present).
- Camera: `ActivityResultContracts.TakePicture` writing to a
  `FileProvider` Uri in `cacheDir` (Day 2) — no CAMERA permission, no
  gallery, no READ_MEDIA_IMAGES.
- **Warm-up trick (Q4):** request the GPS fix *when the camera
  launches*, not after it returns — the 5–15 s in the camera hides the
  fix latency.

## Acceptance criteria

- [ ] Day 1: flow works end-to-end against fakes (simulated photo),
      report appears in Recientes immediately.
- [ ] Day 2: real camera photo round-trips; canceling the camera
      leaves the screen usable.
- [ ] Accuracy >25 m shows the soft-gate warning but **never blocks
      save** (flagged data, not rejected data).
- [ ] Submit failure (airplane mode) → error + Reintentar, composed
      report intact.
- [ ] Coarse-only grant (Android 12+ picker) is treated as denial for
      reporting, with a rationale dialog.

## Pitfalls / notes

- Severity chips: tap again to **deselect** — null is a legitimate
  value and must stay reachable (optional-never-blocks-save).
- `TakePicture` returns false on cancel — don't treat as error.
- Keep the captured file in `cacheDir`; never request storage perms.
- EXIF stays in the uploaded JPEG (forensic backstop, CONTEXT.md) —
  compression in task 06 must not strip capture time.
