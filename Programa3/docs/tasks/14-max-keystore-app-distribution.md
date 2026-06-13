# Task 14 — Max — Keystore, signed release, App Distribution

**Owner:** Max
**Estimated effort:** ~2 h
**Prerequisites:** 00; app feature-complete enough to hand over (08–10)
**Day:** 4 (Jun 14)
**Blocks:** 16 (Melanie needs the distributed build)
**Status:** ready for Melanie (2026-06-12) — signed APK staged at
`docs/entrega/app-release.apk`, App Distribution release
`31cvis3ha3p2o` uploaded to group `testers`, live rules deployed,
Anonymous Auth verified, Maps release SHA-1 added. Pending external
input: Melanie's Google email must be added to `testers` if she does
not already have access.

## Goal

Grilling Q17: signed release APK distributed to Melanie **through
Firebase App Distribution** — the distribution channel is itself the
test provisioning. Signed APK also attached to the entrega as grader
fallback.

## Steps / Outputs

1. Keystore: `keytool -genkeypair` → `~/keystores/bachewatch-release.jks`
   (outside the repo, like `local.properties`; credentials in
   `local.properties`: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
2. `app/build.gradle.kts`: `signingConfigs.release` reading those
   properties (skip config if absent so other machines still build
   debug).
3. **Release SHA-1 → Maps API key restriction** (Cloud console). This
   is the pre-written trap (Q17): skipping it ships Melanie a build
   with a blank map. `keytool -list -v -keystore …` for the SHA-1.
4. Enable App Distribution in console; add Melanie (tester group
   `testers`); add the `firebase-appdistribution` gradle plugin or use
   `firebase appdistribution:distribute app-release.apk --app <APP_ID>
   --groups testers --release-notes "..."`.
5. Smoke the release APK locally first: `adb install -r
   app-release.apk` — minify is **off** (plan), so no R8 surprises,
   but verify maps + auth + camera once.

## Acceptance criteria

- [ ] Melanie receives the invite, installs via App Distribution, and
      the **map renders tiles** on her phone (proves step 3).
- [ ] Anonymous auth + report flow work in the release build.
- [ ] Keystore + passwords not in git (`git status` clean of them);
      documented recovery note (where the keystore lives) in
      `docs/` for future-Max.
- [ ] APK copied to `docs/entrega/` staging for task 18.

## Pitfalls / notes

- App Distribution serves the **app id** registered in task 00 — same
  `google-services.json`, no new Firebase app needed for release
  builds (same package name).
- If Melanie's install hangs at "downloading": she must accept the
  tester invite in the same Google account as Play Services — known
  App Distribution gotcha, put it in the guía (task 15).
