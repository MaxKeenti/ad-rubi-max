# Task 13 — Max — Unit tests (viewport filter + ViewModels)

**Owner:** Max
**Estimated effort:** ~2.5 h
**Prerequisites:** 02 (fakes), 06 (the extracted filter function)
**Day:** 4 (Jun 14)

## Goal

The JVM test layer from grilling Q16: the geohash viewport
false-positive filter (the one piece of real logic in the query path)
and ViewModel states against fakes.

## Inputs

- `docs/work-division.md` §2
- The pure filter function extracted in task 06
- P2 precedent for coroutine test setup (`kotlinx-coroutines-test`)

## Outputs

`app/src/test/java/com/example/bachewatch/`:

- `GeoBoundsTest` / `ViewportFilterTest` — points inside, outside-but-
  in-geohash-cell (the false positive ADR-0002 documents), boundary
  lat/lng equality, antimeridian *not* handled (CDMX app — assert the
  documented assumption, don't implement it).
- `ReportarViewModelTest` — fix loading → success/timeout states;
  severity deselect to null; descripción truncated at 200; submit
  failure keeps state + exposes retry; submit success emits enviado.
- `MapaViewModelTest` — viewport change re-queries (fake records
  calls); heatmap toggle preserves reportes.
- `FakeReporteRepositoryTest` — confirmar idempotence, eliminar
  own/<24 h enforcement (the fakes must keep telling the truth — they
  are Melanie's demo and the ViewModels' test bed).
- Replace template `ExampleUnitTest`/`ExampleInstrumentedTest`.

## Acceptance criteria

- [ ] `./gradlew testDebugUnitTest` green, no emulator, no network.
- [ ] Test names readable as a spec (backtick sentence style).
- [ ] Runs in Melanie's release gate alongside task 12's suite.

## Pitfalls / notes

- `MainDispatcherRule` + `runTest`; no `Thread.sleep`, no real delays —
  fakes' `delay()` is virtual under `runTest`.
- Don't unit-test Compose UI (androidTest is out of scope per plan);
  the ViewModel state machine is the testable surface.
