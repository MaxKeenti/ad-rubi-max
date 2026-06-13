# Task 17 — Melanie — Release gate: re-run automated suites

**Owner:** Melanie (en la máquina de Max o con el repo clonado)
**Estimated effort:** ~45 min
**Prerequisites:** 12, 13 verdes según Max; 16 terminada
**Day:** 15 de junio

> Para la explicación narrativa en español: `/explicar-tarea 17`

## Goal

Una segunda persona reproduce las suites automatizadas antes de la
entrega (Q16) — si solo corren en la cabeza de Max, no son evidencia.

## Steps

```sh
cd Programa3
./gradlew testDebugUnitTest
firebase emulators:exec --only firestore,storage "npm --prefix tests/rules test"
```

Captura de pantalla de ambos resultados verdes →
`docs/screenshots/release-gate-unit.png` y `release-gate-rules.png`.

## Acceptance criteria

- [ ] Ambos comandos verdes, ejecutados por Melanie (no por Max).
- [ ] Capturas guardadas con fecha visible.
- [ ] Si algo falla: se detiene la entrega y se avisa a Max — el gate
      existe exactamente para esto.

## Pitfalls / notes

- Los emuladores de Firebase necesitan Java y descargan en el primer
  arranque — hacerlo con tiempo, no a las 11 pm del día 15.
- `npm install` dentro de `tests/rules/` la primera vez.
