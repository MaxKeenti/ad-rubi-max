# Task 03 — Max — App shell: navigation + screen stubs

**Owner:** Max
**Estimated effort:** ~2 h
**Prerequisites:** 02
**Day:** 1 (Jun 11)
**Blocks:** 04, 07, 09, 10
**Status:** done (2026-06-11)

## Goal

Navigable three-screen skeleton running **against fakes** — the Day-1
checkpoint. Map-first per grilling Q13: Mapa is home, FAB → Reportar,
top-bar action → Recientes.

## Inputs

- `docs/implementation_plan.md` §4 (screen map)
- `docs/CONTEXT.md` (Spanish UI vocabulary: Reportar, Recientes,
  Confirmar, severidad chips)

## Outputs

- `ui/navigation/Screen.kt` — sealed routes: `mapa`, `reportar`,
  `recientes`.
- `ui/navigation/BacheWatchNavGraph.kt` — NavHost, Mapa = start.
- `MainActivity.kt` — `@AndroidEntryPoint`, edge-to-edge, NavGraph.
- `ui/mapa/MapaScreen.kt` + `MapaViewModel` — ViewModel already
  observes `observarViewport(GeoBounds.CDMX)`; body is a placeholder
  card ("mapa en tarea 07") showing the live fake-report count, FAB
  "Reportar", top-bar icon → Recientes. The GoogleMap composable
  arrives in task 07 — keeps Day 1 free of API-key dependencies.
- `ui/recientes/RecientesScreen.kt` + ViewModel — `recientes(50)` into
  a LazyColumn: Coil photo, severity chip (leve verde / moderado ámbar /
  severo rojo), descripción, relative time, confirm count.
- `data/util/TiempoRelativo.kt` — `DateUtils.getRelativeTimeSpanString`
  (locale gives Spanish for free).
- Severity colors in `ui/theme/Color.kt`.

## Acceptance criteria

- [x] App launches to Mapa, navigates to Reportar and Recientes and
      back, with **no Firebase project configured**.
- [x] Recientes renders the 12 seeded reportes newest-first with
      images, chips, and "hace N horas" strings.
- [x] Deleted seeds (if any) never render.

## Pitfalls / notes

- Detail bottom sheet is task 09; Recientes rows are not yet tappable.
- Don't put map state in NavBackStackEntry — camera position will be
  remembered inside MapaScreen (task 07).
- Coil needs `INTERNET` (manifest, task 01) and picsum URLs need a
  network — fine for dev; Melanie's emulator demo also has network.
