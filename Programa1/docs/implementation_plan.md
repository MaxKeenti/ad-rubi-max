# Mangos USA — Current Implementation Overview

**Updated:** 2026-06-01
**Course:** 6NM61 Programacion Movil, UPIICSA
**Team:** Gonzalez Calzada Maximiliano, Sosa Montoya Melanie Rubi

This document is the current implementation snapshot. Earlier planning
decisions remain in `docs/grilling-session-2026-05-26.md`, ADRs,
and task specs, but the codebase now reflects the delivered app rather than
the original proposed build plan.

---

## 1. Delivered Scope

The Android app implements an offline-first purchase receiving workflow for
Mangos USA:

- Email/password login through Firebase Authentication.
- Role-aware navigation for Operator and Admin users.
- Dashboard with today's tons, today's purchase count, active supplier count,
  recent purchases, logout, and quick purchase FAB.
- Purchase capture/edit/history with optional price, `UNREGISTERED` supplier
  support, soft-delete, 24h Operator edit window, and pending-sync badges.
- Supplier list/add/edit/deactivate for Admin users.
- User-management screen for Admin users:
  - create Operator,
  - create another Admin with Admin re-authentication,
  - promote Operator to Admin while preserving historical purchase attribution.
- Reports with today's tons, today's spend, count of price-less entries, and
  top 5 suppliers for the current month.
- Firestore Security Rules and emulator tests for server-side authorization.
- Formal Spanish deliverables compiled with Typst into one PDF.

## 2. Explicitly Deferred

| Deferred item | Reason |
|---|---|
| Settings screen | Logout lives in Dashboard overflow; no other settings are needed for delivery. |
| Public self-registration | Rejected by domain/security policy; Admins create accounts. |
| Charts in Reports | Text totals/top-5 cover the delivery requirement; Vico can be added later. |
| Date-range filter in History | Supplier filter shipped; date-range is additive. |
| Shared-tablet auth | ADR-0001 keeps one account per Operator phone for this release. |
| Deleted-purchase recovery UI | Soft-delete exists for audit; no browsing/restore UI in v1. |
| CSV/Excel export | Useful follow-up, outside course scope. |

## 3. Tech Stack

| Layer | Current implementation |
|---|---|
| Android | Kotlin 2.2.10, AGP 8.10.1, Jetpack Compose, Material 3 |
| SDK targets | `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36` |
| Dependency injection | Hilt 2.56.2 |
| Firebase | Auth, Firestore, Functions, Google Services plugin |
| Offline | Firestore local cache and `metadata.hasPendingWrites` |
| Backend functions | TypeScript, Node 20, Firebase Functions v2, Admin SDK |
| Security tests | `@firebase/rules-unit-testing` with Node test runner |
| Documentation | Markdown content rendered by Typst + cmarker |

## 4. Architecture

The code uses MVVM with repository interfaces as the domain boundary.
ViewModels depend on interfaces, not Firebase classes.

```text
UI (Compose Screens)
        |
ViewModels
        |
Repository interfaces
        |
Firestore/Auth/Functions implementations
```

Implemented repository contracts:

- `AuthRepository`
- `SupplierRepository`
- `PurchaseRepository`
- `UserAdminRepository`

Implemented data providers:

- `data/repository/firestore/*` for Auth, Suppliers, and Purchases.
- `data/repository/functions/*` for Admin user operations via callable
  Functions, with Spark-plan REST fallback.
- `data/repository/fake/*` kept in the repo as development scaffolding, but
  not bound in `AppModule`.

`AppModule` currently binds the real implementations:

- `AuthRepositoryFirestoreImpl`
- `SupplierRepositoryFirestoreImpl`
- `PurchaseRepositoryFirestoreImpl`
- `UserAdminRepositoryFunctionsImpl`

## 5. Data Model

Firestore collections:

```text
users/{uid}
suppliers/{supplierId}
purchases/{purchaseId}
```

Important decisions now reflected in code:

- `users/{uid}` uses the Firebase Auth `uid`.
- `disabledAt`, `retiredAt`, `promotedToUid`, and `promotedFromUid` preserve
  promotion history without rewriting purchases.
- `suppliers/UNREGISTERED` is the reserved escape hatch for trucks from
  suppliers not yet in the catalog.
- Purchases use three time fields:
  - `date`: received-at date chosen by the user,
  - `enteredAt`: client time when the user pressed save,
  - `serverWrittenAt`: server timestamp used for the 24h edit window.
- `dateKey` is a local-day `"YYYY-MM-DD"` bucket in America/Mexico_City.
- Money is stored as nullable `Long` centavos in
  `pricePerTonCentavos`.
- Soft-delete writes `deletedAt = Timestamp.now()` and `deletedBy = uid`;
  reads filter `deletedAt == null`.

See `docs/entrega/04-modelo-de-datos/content.md` and
`docs/CONTEXT.md` for full semantics.

## 6. Security

Authorization is server-side in `firestore.rules`.

Policy summary:

- Active users can read their own user doc; active Admins can read user docs.
- Client writes to existing user docs are limited to `displayName`, except
  the exact Admin-only retirement/promotion fields needed by the Spark
  fallback.
- Active Admins can create user docs only with the exact expected creation
  shape and server timestamp.
- Only Admins write suppliers.
- Active users create their own purchases only when
  `serverWrittenAt == request.time`.
- Admins can edit/delete any purchase.
- Operators can edit/delete their own live purchase only within 24 hours of
  `serverWrittenAt`, and only operational fields are writable.
- Users with `disabledAt` or `retiredAt` cannot continue operating, even if
  an old Auth token still exists.

User-account privileged work has two paths:

- **Preferred Blaze path:** callable Functions backed by Firebase Admin SDK.
- **Spark fallback:** Android uses Identity Toolkit REST for Auth account
  creation/deletion and Firestore writes constrained by rules.

## 7. Offline Fixes That Shipped

Two regressions found on the Xiaomi 15T run were fixed before final docs:

- **Soft-delete refresh:** `deletedAt` changed from
  `FieldValue.serverTimestamp()` to client `Timestamp.now()` so active-list
  queries drop deleted rows immediately from the local cache.
- **Offline purchase save:** `PurchaseRepositoryFirestoreImpl.add()` no
  longer awaits the Firestore server ack. It queues the write, returns the
  new id, and uses listeners for eventual sync status.
- **Pending badge refresh:** `observeRecentWithPending()` uses
  `MetadataChanges.INCLUDE` so the `hasPendingWrites: true -> false`
  transition emits and clears the badge.

## 8. Build And Verification Commands

```sh
# Android APK
cd Programa1
./gradlew :app:assembleDebug

# Android unit tests
./gradlew :app:testDebugUnitTest

# Firestore rules tests
cd ..
firebase emulators:exec --only firestore "npm --prefix tests/rules test"

# Functions TypeScript build
npm --prefix functions run build

# Documentation PDF
cd docs/entrega
just bundle
```

Primary output files:

- Android APK:
  `app/build/outputs/apk/debug/app-debug.apk`
- Formal PDF:
  `docs/entrega/entrega-completa.pdf`

## 9. Documentation Map

| Need | File |
|---|---|
| Domain language | `docs/CONTEXT.md` |
| Architecture decisions | `docs/adr/` |
| Formal deliverables | `docs/entrega/` |
| Manual test runbook | `docs/guia-pruebas-melanie.md` |
| Historical work split | `docs/work-division.md` |
| Historical task specs | `docs/tasks/` |
