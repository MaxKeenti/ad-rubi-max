# Task 15 — Max — Draft guía de pruebas (Melanie's runbook)

**Owner:** Max (drafts) → Melanie (executes, task 16)
**Estimated effort:** ~2 h
**Prerequisites:** 08–10 feature-complete (steps must match the app)
**Day:** 4 (Jun 14)

## Goal

`docs/guia-pruebas-melanie.md` — numbered, checkbox-style runbook in
**Spanish** (Melanie's working language; P1 precedent) covering what
automation can't: real GPS, real camera, real mobile data, on a real
street.

## Inputs

- `Programa1/docs/guia-pruebas-melanie.md` (format precedent)
- `docs/work-division.md` §3 (the committed scenario list)
- `docs/CONTEXT.md` (vocabulary — the guía uses domain terms)

## Outputs

Runbook sections, each step = action + expected result + evidence
column (screenshot name):

1. **Instalación** via App Distribution (incl. the same-Google-account
   gotcha from task 14).
2. **Flujo de reporte completo** en la calle: cámara → indicador GPS
   (anotar ±m bajo cielo abierto vs entre edificios — urban canyon) →
   chips severidad → enviar con datos móviles.
3. **Soft accuracy gate**: forzar mal fix (arrancar bajo techo),
   verificar advertencia pero guardado permitido.
4. **Retry path**: modo avión al enviar → error + Reintentar → quitar
   modo avión → Reintentar funciona, sin duplicados.
5. **Permisos**: denegar ubicación → app completa solo-lectura, FAB
   explica; precisa vs aproximada (Android 12+) → aproximada = denegada.
6. **Cancelar cámara** → pantalla usable.
7. **Confirmar** (en un reporte de Max) → contador +1, botón bloqueado,
   reabrir → sigue bloqueado.
8. **Eliminar propio** <24 h → desaparece de mapa y Recientes;
   verificar ausencia de Eliminar en reportes ajenos.
9. **Heatmap**: toggle Zonas, severo brilla más que leve.
10. **Capturas para la entrega** — checklist of the screenshots task
    18 needs (map, heatmap, reportar flow, detail sheet, recientes,
    permission dialog).

Plus a **Resultados** column template (✓/✗/notas) and a findings
section header so results land in one file.

## Acceptance criteria

- [ ] Every scenario in work-division §3 has numbered steps.
- [ ] Each step names its evidence artifact (screenshot filename
      convention `NN-descripcion.png` → `docs/screenshots/`).
- [ ] Dry-run by Max on the emulator: steps match the real UI labels
      (no "tap the button that doesn't exist").

## Pitfalls / notes

- Write steps against UI **labels**, not implementation ("toca
  *Reportar*", not "toca el FAB").
- Include expected logcat-free checks only — Melanie tests from the
  phone, no adb.
