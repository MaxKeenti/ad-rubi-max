# Task 18 — Max — Entrega HTML (estilo Programa2)

**Owner:** Max (con capturas y resultados de Melanie)
**Estimated effort:** ~3 h
**Prerequisites:** 16, 17 (evidencia lista); todo lo demás congelado
**Day:** 5 (Jun 15)
**Status:** drafted (2026-06-12) — Spanish HTML shell and all ten
sections are generated under `docs/entrega/html/`, with portada at
`index.html` and APK link wired. Pending final pass after Melanie adds
manual screenshots and release-gate evidence.

## Goal

Spanish delivery documents as **HTML mirroring Programa2's
implementation** — NOT Typst (P1's approach). Working docs in `docs/`
stay canonical English; the entrega is derived, not maintained twice.

## Inputs

- `Programa2/index.html` + `Programa2/docs/*.html` (the structure and
  styling to reuse — copy the CSS/layout approach wholesale)
- All of `Programa3/docs/` (canonical sources)
- `docs/screenshots/` (Melanie's evidence), signed APK (task 14)

## Outputs

`Programa3/` mirroring P2's shape: root `index.html` (portada +
navegación) linking translated sections:

1. Resumen ejecutivo (qué es BacheWatch, stack, equipo)
2. Requerimientos y suposiciones explícitas (plan §1)
3. Arquitectura (MVVM + seam, ADRs resumidos, diagrama)
4. Modelo de datos (plan §2 + reglas §3)
5. Decisiones arquitectónicas (ADR-0001, ADR-0002 traducidos)
6. Glosario (CONTEXT.md traducido)
7. Manual de usuario (con capturas de Melanie)
8. Plan y resultados de pruebas (suite de reglas + unit + guía con
   resultados ✓/✗ + capturas del release gate)
9. Conclusiones
10. Distribución: link App Distribution + APK firmado adjunto

## Acceptance criteria

- [ ] Navega bien en un teléfono (ese era el argumento para HTML).
- [ ] Cada sección traza a su fuente canónica en `docs/` (comentario
      HTML con la ruta).
- [ ] Capturas embebidas, no linkeadas a rutas locales rotas.
- [ ] Nombres completos del equipo y grupo 6NM61 en la portada.
- [ ] Todo en español; sin restos de markdown sin renderizar.

## Pitfalls / notes

- Reuse P2's CSS verbatim first, adjust second — Jun 15 is buffer, not
  a design day.
- The APK must be the **same build Melanie tested**, not a fresh one
  compiled after her run (or the release gate proved nothing).
- Check `git status` before submitting: keystore, `local.properties`,
  Maps key must not be in the repo.
