# Task 00 — Max — Firebase + Google Cloud project

**Owner:** Max
**Estimated effort:** ~1 h (mostly console clicking)
**Prerequisites:** none
**Day:** 1 (Jun 11)
**Blocks:** 06 (real repository), 07 (map), 14 (distribution)
**Status:** in progress (2026-06-12) — step 1 done via CLI (project
`bachewatch-upiicsa`, app `1:1008157923999:android:2a6d136350f1b16bcee0aa`,
`app/google-services.json` committed-ready). Steps 2–6 are console-only
and still pending: Anonymous Auth, Firestore (API not yet enabled),
Storage/Blaze, Maps SDK + API key.

## Goal

One Google Cloud project that serves Firebase (Anonymous Auth,
Firestore, Storage, App Distribution) **and** the Google Maps SDK —
same-project keeps native Android map loads unbilled (grilling Q10).

## Inputs

- `docs/adr/0001-anonymous-auth.md`
- `docs/implementation_plan.md` §2–3 (what the services must support)
- Precedent: Programa1's Firebase setup (`Programa1/firebase.json`)

## Steps / Outputs

1. **Project** (CLI is already logged in as maxgonzalezcalzada@gmail.com):
   ```sh
   firebase projects:create bachewatch-upiicsa --display-name "BacheWatch"
   firebase apps:create android com.example.bachewatch --project bachewatch-upiicsa
   firebase apps:sdkconfig android --project bachewatch-upiicsa   # → app/google-services.json
   ```
2. **Anonymous Auth** — Console → Authentication → Sign-in method →
   enable *Anonymous*. (No CLI for this.)
3. **Firestore** — Console → create database, production mode. Region:
   `nam5 (us-central)` — Programa1's choice; keep consistent.
4. **Storage** — Console → Storage → create default bucket.
   ⚠ New projects need the **Blaze plan** for a default bucket
   (policy since Oct 2024). Free quotas still apply; add a budget
   alert at $1 if upgrading.
5. **Maps SDK for Android** — Google Cloud console, same project:
   enable the API, create an API key, restrict it to Android apps with
   package `com.example.bachewatch` + **debug** SHA-1
   (`./gradlew signingReport`). Release SHA-1 is added in task 14 —
   forgetting it blanks the map in exactly the distributed build.
6. Put the key in `Programa3/local.properties` as `MAPS_API_KEY=...`
   (read by `app/build.gradle.kts`; never committed).

## Acceptance criteria

- [x] `app/google-services.json` exists; `./gradlew assembleDebug` passes.
- [ ] Anonymous sign-in works (verified end-to-end in task 06).
- [ ] Firestore + Storage visible in console, locked rules (real rules in task 11).
- [ ] Map renders tiles in debug build (verified in task 07).

## Pitfalls / notes

- `google-services.json` is committed (it is not a secret); the Maps
  API key and keystore are **not** — they live in `local.properties`
  like P2's Gemini key.
- Don't enable App Distribution yet; task 14 owns it.
- If Blaze is blocked, fallback: store photos as Base64 in Firestore?
  **No** — that breaks the rubric's FireStorage line. Escalate instead.
