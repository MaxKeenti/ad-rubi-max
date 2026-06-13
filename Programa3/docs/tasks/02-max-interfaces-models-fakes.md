# Task 02 — Max — Models, interfaces, fakes, Hilt module

**Owner:** Max
**Estimated effort:** ~2.5 h
**Prerequisites:** 01
**Day:** 1 (Jun 11)
**Blocks:** all UI work, all real implementations
**Status:** done (2026-06-11)

## Goal

The contract seam (grilling Q15): three interfaces + in-memory fakes
seeded with CDMX potholes. The seam is the **testability device** —
ViewModels unit-test without Firebase, and Melanie gets a deterministic
zero-credential app. UI never sees Firebase/GeoFire types (sole
exception, P1 precedent: `com.google.firebase.Timestamp` in the model).

## Inputs

- `docs/implementation_plan.md` §2 (frozen schema), §4 (interfaces)
- `docs/CONTEXT.md` (domain semantics — read before naming anything)
- Precedent: `Programa1/.../data/repository/fake/*`, `data/di/AppModule.kt`

## Outputs

`data/model/`: `Reporte` (schema §2, `severidad: Severidad?`),
`Severidad` (LEVE/MODERADO/SEVERO + heatmap `peso` 1/2/3, unset 1.5),
`LocationFix(lat, lng, accuracyMeters)`,
`GeoBounds(swLat, swLng, neLat, neLng)` + `contains()` (our own bounds
type so interfaces stay Maps-SDK-free and unit-testable).

`data/repository/ReporteRepository.kt`:

```kotlin
interface ReporteRepository {
  suspend fun crearReporte(fotoUri: Uri, fix: LocationFix,
      severidad: Severidad?, descripcion: String?): Result<String>
  fun observarViewport(bounds: GeoBounds): Flow<List<Reporte>>
  fun recientes(limit: Int = 50): Flow<List<Reporte>>
  suspend fun confirmar(reporteId: String): Result<Unit>
  suspend fun yaConfirmo(reporteId: String): Boolean  // disables the button (CONTEXT.md)
  suspend fun eliminar(reporteId: String): Result<Unit>
}
```

`data/location/LocationProvider.kt` — `suspend fun fixActual():
Result<LocationFix>` (Result, not the plan's bare type: permission
denial and timeout are expected outcomes, not exceptions).

`data/auth/SesionAnonima.kt` — `val uid: StateFlow<String?>`,
`suspend fun ensureSignedIn(): Result<String>`.

Fakes (same packages, `Fake` prefix): `FakeReporteRepository`
(MutableStateFlow seeded with ~12 CDMX reportes — UPIICSA/Iztacalco,
Centro, Roma, Coyoacán…; picsum.photos fotoUrls; two owned by the fake
uid, one inside and one outside the 24 h window so Eliminar is
demoable), `FakeLocationProvider` (~1.5 s delay, jittered fix near
UPIICSA, accuracy 8–30 m so the soft gate is visible),
`FakeSesionAnonima` (fixed uid). `data/di/AppModule.kt` binds fakes;
real bindings land commented-out, flipped in tasks 05/06.

## Acceptance criteria

- [x] `./gradlew assembleDebug` passes; Hilt graph resolves.
- [x] Fake viewport filtering respects `GeoBounds.contains`.
- [x] Fake `confirmar` is idempotent per uid (second call fails / no-op)
      and bumps `confirmCount` — same semantics the rules enforce later.
- [x] Fake `eliminar` enforces own + <24 h.

## Pitfalls / notes

- **Filter `deletedAt == null` inside the repository** (P1 lesson).
- `recientes` ignores viewport — it's `orderBy serverWrittenAt desc`.
- Seeds use staggered ages (30 min – 4 days) so relative dates and the
  24 h window are visible in the demo.
- Severity weights live on the enum **now**; the heatmap (task 08)
  must not invent its own mapping.
