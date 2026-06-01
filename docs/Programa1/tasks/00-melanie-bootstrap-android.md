# Task 00 — Melanie — Bootstrap Android project

**Owner:** Melanie
**Estimated effort:** ~2 hours (most of which is Gradle frustration; budget generously)
**Prerequisites:** `00-max-firebase-project` complete (need `google-services.json`)
**Day:** 0
**Blocks:** everything in Melanie's lane on day 1+

## Goal

Take the empty Compose scaffold and turn it into a buildable project with
Firebase, Hilt, and Compose Navigation wired up. Get one successful
`./gradlew assembleDebug` and one successful launch on the emulator
showing a placeholder Compose screen. No real screens yet.

## Inputs

- `docs/Programa1/implementation_plan.md` Component 1 (Project Setup) and Component 10 (Navigation & Main Activity)
- `docs/Programa1/entrega/03-arquitectura/content.md` § 2 (Stack tecnológico) and § 3 (Package structure)
- Current state of `Programa1/` (empty Compose scaffold)

## Outputs

Files to modify:

- `Programa1/build.gradle.kts` (project-level) — add Google Services plugin, Hilt plugin
- `Programa1/app/build.gradle.kts` — add Firebase BOM + Firestore + Auth, Hilt, Navigation Compose, kapt, apply plugins
- `Programa1/settings.gradle.kts` — version catalog entries
- `Programa1/app/src/main/AndroidManifest.xml` — add INTERNET permission, set application class to `MangosApp`

Files to create:

- `Programa1/app/src/main/java/com/example/mangos/MangosApp.kt` — `@HiltAndroidApp class MangosApp : Application()`
- `Programa1/app/src/main/java/com/example/mangos/MainActivity.kt` — replace default with `@AndroidEntryPoint` Compose activity hosting a placeholder
- Empty package directories (Kotlin doesn't need them but layout convention helps):
  - `data/model/`, `data/repository/`, `data/util/`, `data/di/`
  - `ui/auth/`, `ui/dashboard/`, `ui/purchases/`, `ui/suppliers/`, `ui/reports/`, `ui/navigation/`
- A `.gitkeep` in each empty package directory if you want them tracked.

Suggested dependency versions (May 2026 stable):

```kotlin
// app/build.gradle.kts
implementation(platform("com.google.firebase:firebase-bom:33.5.0"))
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")

implementation("com.google.dagger:hilt-android:2.52")
kapt("com.google.dagger:hilt-compiler:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

implementation("androidx.navigation:navigation-compose:2.8.5")
```

Adjust if a newer stable exists.

## Acceptance criteria

- [ ] `./gradlew assembleDebug` ends in `BUILD SUCCESSFUL`.
- [ ] App installs and launches on emulator showing a Compose placeholder (a `Text("Mangos USA — bootstrap OK")` is fine).
- [ ] No Hilt or Firebase initialization crashes in logcat.
- [ ] `MangosApp` is the Application class (verified by setting a log line in `onCreate`).
- [ ] Branch `rubi` pushed; Max can pull and inspect.

## Pitfalls / notes

- **Hilt + Compose + kapt** combo bites. If you see "Unresolved reference: HiltAndroidApp" the kapt processor isn't wired. Make sure `kotlin-kapt` plugin is applied and `kapt(...)` is in dependencies.
- **Firebase BOM** is non-negotiable — never pin individual Firebase versions. The BOM keeps them compatible.
- **`google-services.json` path** must be `Programa1/app/google-services.json` (next to `app/build.gradle.kts`).
- Current Gradle config after implementation: `minSdk = 26`,
  `targetSdk = 36`, and `compileSdk = 36`. Keep requirements and manual
  aligned with those values.
- This is **the most failure-prone task of the project**. If you get stuck, push WIP and grab Max — he can read the Gradle error from the commit and suggest the fix.
- Once this lands, Max's `00-max-draft-interfaces` task can start. The interface review sync happens once both day-0 tasks are landed.
