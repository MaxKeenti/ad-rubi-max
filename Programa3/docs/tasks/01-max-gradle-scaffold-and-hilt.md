# Task 01 — Max — Align gradle scaffold + Hilt

**Owner:** Max
**Estimated effort:** ~1.5 h
**Prerequisites:** none (parallel with 00)
**Day:** 1 (Jun 11)
**Blocks:** everything else
**Status:** done (2026-06-11)

## Goal

Turn the freshly generated Android Studio project into the plan's
stack — and de-risk the week by pinning **Programa1's known-good
toolchain** instead of the bleeding-edge one the template generated.

## Inputs

- `Programa1/gradle/libs.versions.toml` + `Programa1/app/build.gradle.kts`
  (the proven AGP 8.10.1 / Kotlin 2.2.10 / Hilt 2.56.2 / KSP combo)
- `docs/implementation_plan.md` header (stack list)

## Outputs

- `gradle/wrapper/gradle-wrapper.properties` — Gradle **8.14.3** (the
  template generated 9.4.1 + AGP 9.2.1, untested with Hilt; don't fight
  that war this week).
- `gradle/libs.versions.toml` — P1's versions plus: `maps-compose`,
  `android-maps-utils`, `play-services-location`, `play-services-maps`,
  `geofire-android-common`, `coil-compose`, `firebase-storage`.
- Root `build.gradle.kts` — declare kotlin-android, google-services,
  ksp, hilt plugins `apply false`.
- `app/build.gradle.kts` — `minSdk = 26` (plan; template said 36!),
  `compileSdk/targetSdk = 36`, plugins applied, deps wired,
  `MAPS_API_KEY` manifest placeholder read from `local.properties`
  (default `""` so the build never breaks), google-services plugin
  applied **only if `app/google-services.json` exists** so task 00
  isn't a build-blocker.
- `BacheWatchApp.kt` — `@HiltAndroidApp`, registered in the manifest.
- Manifest: `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`,
  `com.google.android.geo.API_KEY` meta-data.

## Acceptance criteria

- [x] `./gradlew assembleDebug` passes without `google-services.json`.
- [x] Hilt generates `Hilt_BacheWatchApp` (KSP ran).
- [x] minSdk 26 / target 36 in the merged manifest.

## Pitfalls / notes

- **Never request the `CAMERA` permission** — system camera intent
  (grilling Q4) needs none; adding it triggers Play-style scrutiny in
  the rubric review for nothing.
- Coarse location is requested alongside fine because Android 12+
  shows the dual picker; the *policy* (coarse = denied for reporting)
  lives in task 05, not the manifest.
- The AGP-9 template DSL (`compileSdk { version = release(36) {...} }`)
  must be rewritten to plain `compileSdk = 36` for AGP 8.
