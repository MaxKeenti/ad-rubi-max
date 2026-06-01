# Mangos USA — Mobile Purchase Tracking

Android app for the UPIICSA Programacion Movil final project. The app
replaces a warehouse whiteboard/sticky-note workflow with an offline-first
purchase receiving log for Mangos USA.

## What It Does

- Operators record mango truck arrivals from an Android phone at the dock.
- Purchases sync through Firebase Firestore and keep working offline.
- Admins manage suppliers and users from the app.
- Admins can create Operators, create another Admin with re-authentication,
  and promote an Operator to Admin without rewriting historical purchases.
- Firestore Security Rules enforce roles server-side; UI role checks are only
  convenience.
- Reports show today's tons, today's spend in MXN, entries without price, and
  the top 5 suppliers of the current month.

## Tech Stack

| Area | Technology |
|---|---|
| Android | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM, repository interfaces, Hilt DI |
| Backend | Firebase Auth, Cloud Firestore, Firebase Functions |
| Offline | Firestore local cache and pending-write metadata |
| Tests | Gradle unit tests, Firestore Rules emulator tests |
| Docs | Markdown + Typst |

## Repository Layout

```text
.
├── app/                       # Android app
├── functions/                 # Firebase callable Functions for user admin
├── tests/rules/               # Firestore Security Rules tests
├── docs/entrega/              # Formal Spanish deliverables and Typst sources
├── firestore.rules            # Server-side authorization policy
├── firestore.indexes.json     # Firestore composite indexes
├── firebase.json              # Firebase deploy/emulator config
└── .firebaserc                # Firebase project alias
```

## Build And Test

```sh
# From the workspace root
cd Programa1

# Android debug APK
./gradlew :app:assembleDebug

# Android unit tests
./gradlew :app:testDebugUnitTest

# Firestore rules tests
firebase emulators:exec --only firestore "npm --prefix tests/rules test"

# Firebase Functions TypeScript build
npm --prefix functions run build
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

The formal deliverables live in `docs/entrega/`.

```sh
cd docs/entrega
just bundle
```

This creates the single compiled PDF:

```text
docs/entrega/entrega-completa.pdf
```

Individual deliverables can still be compiled with `just build`.

## Firebase Notes

- `app/google-services.json` identifies the Firebase project for the
  Android SDK. It is not an authorization secret.
- `functions/.env` is intentionally ignored. Copy
  `functions/.env.example` and set `FIREBASE_WEB_API_KEY` before deploying
  Functions to a Blaze project.
- The app includes a Spark-plan fallback for admin user management when
  callable Functions are not deployed, protected by Firestore rules that only
  active Admin users can satisfy.

## Team

- Gonzalez Calzada Maximiliano — 2021601769
- Sosa Montoya Melanie Rubi — 2024601345

Course: 6NM61 Programacion Movil, UPIICSA. Delivery date: June 1, 2026.
