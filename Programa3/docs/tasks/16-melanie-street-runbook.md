# Task 16 — Melanie — Ejecutar la guía de pruebas en la calle

**Owner:** Melanie
**Estimated effort:** ~3 h (incluye salir a la calle con datos móviles)
**Prerequisites:** 14 (build instalado vía App Distribution), 15 (guía)
**Day:** 4–5 (Jun 14–15)

> Para la explicación narrativa en español de esta tarea:
> `/explicar-tarea 16`

## Goal

Ejecutar `docs/guia-pruebas-melanie.md` completa en un teléfono real,
en una calle real, con datos móviles — GPS, cámara y conectividad de
verdad, donde la automatización es ciega (Q16).

## Steps

1. Aceptar la invitación de App Distribution **con la misma cuenta
   Google del teléfono** e instalar el build de release.
2. Recorrer la guía paso a paso, marcando ✓/✗ y anotando observaciones
   en la columna de resultados (especialmente los metros de precisión
   GPS en cielo abierto vs entre edificios).
3. Tomar las capturas que la guía pide, con los nombres de archivo
   indicados, y subirlas a `docs/screenshots/`.
4. Cualquier ✗: anotar pasos exactos para reproducir y avisar a Max el
   mismo día (el Jun 15 es buffer, no día de desarrollo).

## Acceptance criteria

- [ ] Guía completa con resultados en cada paso (sin casillas vacías).
- [ ] Capturas en `docs/screenshots/` con la convención de nombres.
- [ ] Hallazgos reportados a Max con pasos de reproducción.
- [ ] El reporte creado en la calle es visible desde el dispositivo de
      Max (prueba multi-dispositivo real).

## Pitfalls / notes

- Probar **con datos móviles, no WiFi** — el escenario objetivo del
  diseño es "en la banqueta con 4G" (Q7).
- El paso de modo avión: componer el reporte *antes* de activar modo
  avión, enviar, ver el error, desactivar, Reintentar.
- Si el mapa sale en blanco en el build distribuido → avisar de
  inmediato: es la trampa del SHA-1 de release (task 14), no un bug
  de Melanie.
