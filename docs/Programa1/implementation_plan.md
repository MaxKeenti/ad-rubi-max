# 🥭 Mangos USA — Mobile Purchase Tracking App

A Kotlin/Jetpack Compose Android application for **Mangos USA** corporation to digitalize their daily mango purchase tracking process. Replaces a physical whiteboard with sticky notes.

**Due date:** June 1st, 2026 · **Team:** 2 members · **Course:** 6NM61 Programación Móvil (UPIICSA)

---

## User Review Required

> [!IMPORTANT]
> **Firebase Project Setup:** You'll need to create a Firebase project in the [Firebase Console](https://console.firebase.google.com/) and download the `google-services.json` file. I'll guide you through this step during development.

> [!WARNING]
> **Scope (post-grilling, 2026-05-26):** Original plan was 16h optimistic; realistic re-estimate after design pass is 22–26h. Cuts and trims applied:
> - **CUT** Settings screen → logout moves to Dashboard top-bar.
> - **CUT** public Register screen → Admin creates accounts in Firebase
>   Console for v1; upcoming work adds authenticated Admin-only account
>   management inside the app.
> - **TRIM** Reports → no Vico charts. Today's total tons + top-5 suppliers by tonnage (text). Add charts back if time permits.
> - **TRIM** Purchase History → single list, newest-first, supplier filter only. No date-range filter.
> - **KEEP** Edit Purchase / Edit Supplier flows (required by 24h Operator typo-fix window and UNREGISTERED supplier reconciliation).
>
> The domain model and authorization decisions captured in `CONTEXT.md` + ADRs 0001/0002 supersede any conflicting field-level details below until this plan is re-synced.

---

## Architecture Overview

### Pattern: MVVM + Clean Architecture (Layered)

```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│  (Compose Screens + ViewModels)              │
├─────────────────────────────────────────────┤
│              Domain Layer                    │
│  (Use Cases / Business Logic)                │
├─────────────────────────────────────────────┤
│               Data Layer                     │
│  (Repositories → Firebase Firestore/Auth)    │
└─────────────────────────────────────────────┘
```

**Justification (for your professor):**
- **MVVM** separates UI state from business logic, making the app testable and maintainable
- **Clean Architecture layers** enforce dependency inversion — the UI doesn't know about Firebase directly
- **Repository pattern** abstracts the data source, making it easy to swap Firebase for another backend (scalability!)
- **Firestore offline persistence** provides automatic local caching without extra code

### Tech Stack
| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Backend | Firebase Firestore (NoSQL) |
| Auth | Firebase Authentication (Email/Password) |
| Offline | Firestore built-in offline persistence |
| Navigation | Jetpack Navigation Compose |
| DI | Hilt (Dagger) |
| Reports | Text-only summary cards; charts deferred |
| State Management | StateFlow + Compose State |

---

## Data Model (Firestore Collections)

> Authoritative source for field semantics is `CONTEXT.md`. This section is the schema-at-a-glance.

### Collection: `users`
```
users/{userId}
├── email: String
├── displayName: String
├── role: String ("admin" | "operator")
├── accountCreatedAt: Timestamp   // serverTimestamp()
├── disabledAt: Timestamp?
├── retiredAt: Timestamp?
├── promotedToUid: String?
└── promotedFromUid: String?
```
No self-registration. Operators cannot register themselves, and Admins cannot
register themselves as Admins. The first Admin is bootstrapped in Firebase
Console; subsequent Operator/Admin creation and Operator promotion happen in
the authenticated Admin-only Users UI through callable Cloud Functions/Admin
SDK. Historical Purchases keep the old Operator `uid`.

### Collection: `suppliers`
```
suppliers/{supplierId}
├── name: String
├── phone: String
├── email: String
├── location: String              // region/state
├── mangoVariety: String
├── isActive: Boolean             // false hides from dock dropdown
├── createdAt: Timestamp          // serverTimestamp()
└── createdBy: String             // admin userId
```
Plus a reserved doc: `suppliers/UNREGISTERED` (name = "Proveedor no registrado", isActive = true). Never deleted; used as the placeholder when an Operator records a truck from an unknown supplier.

### Collection: `purchases`
```
purchases/{purchaseId}
├── supplierId: String                  // ref to suppliers/* (or "UNREGISTERED")
├── supplierName: String                // denormalized at write time — never back-filled
├── supplierNoteFreeform: String?       // only when supplierId == "UNREGISTERED"
├── quantityTons: Double
├── pricePerTonCentavos: Long?          // MXN centavos, NULL = price unknown at dock
├── date: Timestamp                     // received-at: when the truck arrived
├── dateKey: String                     // "YYYY-MM-DD" in America/Mexico_City; day-bucket index
├── createdBy: String                   // Operator userId
├── createdByName: String               // denormalized at write time — never back-filled
├── enteredAt: Timestamp                // client clock when Operator hit save (display)
├── serverWrittenAt: Timestamp          // serverTimestamp() — authoritative for 24h edit rule
├── deletedAt: Timestamp?               // null = live; soft-delete only
└── deletedBy: String?                  // userId of deleter
```
All read queries default to `where deletedAt == null`. See `CONTEXT.md` → "Three timestamps, three purposes" and "Soft-delete schema."

---

## Proposed Changes

### Component 1: Project Setup & Dependencies

#### [MODIFY] [build.gradle.kts](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/build.gradle.kts) (project-level)
- Add Google services plugin and Hilt plugin

#### [MODIFY] [build.gradle.kts](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/build.gradle.kts) (app-level)
- Add dependencies: Firebase (BOM, Firestore, Auth), Hilt, Navigation Compose
- Apply Google services and Hilt plugins

#### [NEW] google-services.json
- Firebase configuration file (user must generate from Firebase Console)

#### [MODIFY] [settings.gradle.kts](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/settings.gradle.kts)
- Add version catalog entries for new dependencies

---

### Component 2: Theme & Design System

#### [MODIFY] [Color.kt](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/src/main/java/com/example/mangos/ui/theme/Color.kt)
- Define mango-inspired color palette:
  - Primary: Rich green (#2E7D32 family)
  - Secondary: Warm mango orange (#FF8F00 family)
  - Tertiary: Golden yellow (#FFC107 family)
  - Surface/Background: Warm off-whites
  - Error: Standard red

#### [MODIFY] [Theme.kt](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/src/main/java/com/example/mangos/ui/theme/Theme.kt)
- Configure Material 3 light color scheme with mango colors
- Set up dynamic color support as fallback

#### [MODIFY] [Type.kt](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/src/main/java/com/example/mangos/ui/theme/Type.kt)
- Set up typography with a clean font (default Material 3 or custom Google Font)

---

### Component 3: Data Layer

#### [NEW] `data/model/User.kt`
- Fields: id, email, displayName, role, accountCreatedAt

#### [NEW] `data/model/Supplier.kt`
- Fields: id, name, phone, email, location, mangoVariety, isActive, createdAt, createdBy

#### [NEW] `data/model/Purchase.kt`
- Fields: id, supplierId, supplierName, supplierNoteFreeform, quantityTons, pricePerTonCentavos (nullable Long), date, dateKey, createdBy, createdByName, enteredAt, serverWrittenAt, deletedAt, deletedBy
- Helper: `pricePerTon: BigDecimal?` derived from centavos at the UI boundary; domain code never uses `Double` for money

#### [NEW] `data/model/UserRole.kt`
- Enum: ADMIN, OPERATOR

#### [NEW] `data/util/MoneyFormatter.kt`
- `Long.centavosToMxnString(): String` — formats `123456` as `"$1,234.56"`
- `String.parseMxnToCentavos(): Long?` — parses user input

#### [NEW] `data/util/DateKey.kt`
- `Timestamp.toDateKey(zone = ZoneId.of("America/Mexico_City")): String` — returns `"YYYY-MM-DD"`
- Used at every Purchase write/edit to keep `dateKey` consistent with `date`

#### [NEW] `data/repository/AuthRepository.kt`
- Interface + Implementation
- Surface: `currentUser`, `signIn()`, `signOut()`, `getUserRole()`
- **No public `register()` / no self-registration** — first Admin is
  bootstrapped in Firebase Console; later account management uses
  `UserAdminRepository` + callable Functions
- Authenticated Admin-only account-management surface creates/manages
  Operators, creates another Admin with Admin re-authentication, and promotes
  Operators through retire/recreate flow
- Wraps Firebase Auth + Firestore user document

#### [NEW] `data/repository/SupplierRepository.kt`
- Interface + Implementation
- Methods: `observeActive()`, `observeAll()` (admin), `getById()`, `add()`, `update()`, `deactivate()` (sets `isActive = false`; no hard delete)
- Wraps Firestore `suppliers` collection; ensures `UNREGISTERED` placeholder doc exists on first admin run

#### [NEW] `data/repository/PurchaseRepository.kt`
- Interface + Implementation
- Surface: `observeById()`, `observeByDateKey()`, `observeByDateRange()`, `observeBySupplier()`, `observeRecent()`, `observeRecentWithPending()`, `getTodaySummary()`, `add()`, `update()`, `softDelete()`
- All read methods filter `deletedAt == null` by default
- `add()` populates `dateKey`, `enteredAt`, `serverWrittenAt`, denormalized names atomically
- Wraps Firestore `purchases` collection

#### [NEW] `data/di/AppModule.kt`
- Hilt module providing Firebase instances and repository bindings

---

### Component 4: UI Layer — Auth (Login only)

#### [NEW] `ui/auth/LoginScreen.kt`
- Email + password fields
- "Iniciar Sesión" button
- Loading state, error handling
- Mango-themed branding at top
- No public "create account" link — admin provisions accounts in Firebase
  Console in v1; upcoming Admin UI handles account creation after login

#### [NEW] `ui/auth/LoginViewModel.kt`
- Manages login state, calls AuthRepository

---

### Component 5: UI Layer — Dashboard

#### [NEW] `ui/dashboard/DashboardScreen.kt`
- **Top bar:** Greeting + date + overflow menu containing **Cerrar sesión** (logout lives here since Settings was cut)
- **Summary cards:** Total tons today, number of purchases today, number of active suppliers
- **Recent purchases list:** Last 5 purchases (filter `deletedAt == null`) with supplier name, quantity, `enteredAt` time
- **Sync indicator:** badge on each pending-sync row (Firestore's `metadata.hasPendingWrites`)
- **FAB:** Quick "Registrar entrada" shortcut → AddEditPurchaseScreen

#### [NEW] `ui/dashboard/DashboardViewModel.kt`
- Queries today's purchases by `dateKey == today(America/Mexico_City)`, computes stats, listens for real-time updates

---

### Component 6: UI Layer — Purchases

#### [NEW] `ui/purchases/AddEditPurchaseScreen.kt`
- Form fields:
  - **Supplier** dropdown — sourced from cached active suppliers; last entry is "Proveedor no registrado" (UNREGISTERED)
  - **Freeform supplier name** — appears only when UNREGISTERED is selected; writes to `supplierNoteFreeform`
  - **Quantity (tons)** — required, > 0
  - **Price per ton (MXN)** — **optional**; entered as `"1234.56"`, persisted as `Long centavos`
  - **Date** — date picker, defaults to today
- Save: writes `enteredAt = now()` (client clock), `serverWrittenAt = serverTimestamp()`, computes `dateKey` from `date`
- Edit mode: enabled if (current user is Admin) OR (current user is creator AND `now - serverWrittenAt < 24h`) — same logic as Firestore rules, mirrored client-side for UX

#### [NEW] `ui/purchases/AddEditPurchaseViewModel.kt`
- Manages form state, save/update logic, edit-window check

#### [NEW] `ui/purchases/PurchaseHistoryScreen.kt`
- List of all live purchases (`deletedAt == null`), newest first
- **Supplier filter** only (no date-range filter — cut for scope)
- Each item shows: supplier, tons, price (or "—" if null), `date`, `enteredAt`
- Tap to edit (subject to edit-window rules); swipe-to-delete is a **soft-delete** (Admin or in-window creator only)
- Hard 50-item limit per fetch with "load more" button

#### [NEW] `ui/purchases/PurchaseHistoryViewModel.kt`
- Loads purchases, handles supplier filter, performs soft-delete

---

### Component 7: UI Layer — Suppliers (Admin Only)

Server-side authorization is enforced by Firestore Security Rules (see Component 11). Hiding the bottom-nav tab for Operators is ergonomics only.

#### [NEW] `ui/suppliers/SupplierListScreen.kt`
- List of all suppliers with name, location, variety, active badge
- FAB to add new supplier
- Tap to edit, swipe to deactivate (sets `isActive = false`; reversible)
- The `UNREGISTERED` placeholder is filtered out of this list (managed by code, not by admin)
- Tab hidden from Operators

#### [NEW] `ui/suppliers/SupplierListViewModel.kt`
- CRUD via SupplierRepository; never hard-deletes

#### [NEW] `ui/suppliers/AddEditSupplierScreen.kt`
- Form: Name, phone, email, location, mango variety
- Validation

#### [NEW] `ui/suppliers/AddEditSupplierViewModel.kt`

---

### Component 8: UI Layer — Reports (text-only, no charts)

Vico cut for scope. If time permits at end of execution, charts can be added back.

#### [NEW] `ui/reports/ReportsScreen.kt`
- **Today's total tons** — big number card (queries by `dateKey`)
- **Today's total spend (MXN)** — derived from `pricePerTonCentavos * quantityTons` summed; excludes price-less rows; shows "(N entradas sin precio)" if any
- **Top 5 suppliers this month** — text list "Hernández — 12.3 t", computed in-app from `dateKey LIKE "2026-05-%"` (or a month-prefix range query). Excludes UNREGISTERED row from top-5 ranking
- No date-range selector in v1

#### [NEW] `ui/reports/ReportsViewModel.kt`
- Aggregates data from PurchaseRepository; tolerates `pricePerTonCentavos == null`

---

### ~~Component 9: UI Layer — Settings~~ **CUT.** Logout lives in Dashboard top-bar overflow menu.

---

### Component 10: Navigation & Main Activity

#### [NEW] `ui/navigation/MangosNavGraph.kt`
- Define all routes and navigation graph
- Conditional navigation based on auth state
- Bottom nav bar items: Dashboard, Purchases, Suppliers (Admin only), Users
  (Admin only), Reports
- No public Register route, no Settings route. Account management is an
  authenticated Admin-only route, not self-registration.

#### [NEW] `ui/navigation/Screen.kt`
- Sealed class defining all screen routes

#### [NEW] `ui/navigation/BottomNavBar.kt`
- Bottom navigation bar composable with icons and labels in Spanish

#### [MODIFY] [MainActivity.kt](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/src/main/java/com/example/mangos/MainActivity.kt)
- Replace default "Hello Android" with navigation host
- Add Hilt Android entry point

#### [NEW] `MangosApp.kt`
- Application class annotated with `@HiltAndroidApp`

#### [MODIFY] [AndroidManifest.xml](file:///Users/moonstone/Source/UPIICSA/Plan%202021/2026%202/6NM61%20Programaci%C3%B3n%20m%C3%B3vil/ad-rubi-max/Programa1/app/src/main/AndroidManifest.xml)
- Add INTERNET permission
- Set application class to MangosApp

---

### Component 11: Firestore Security Rules (NEW — see ADR-0002)

Authorization is enforced server-side. Without this, the admin/operator role split is theatrical.

#### [NEW] `firestore.rules`
- `users/{uid}` — read = own or admin; write = own except `role`; admin can write any
- `suppliers/{id}` — read = any signed-in; write = admin only
- `purchases/{id}` — read = any signed-in; create = signed-in with `createdBy == auth.uid`; update/delete = admin OR (creator AND `request.time - resource.data.serverWrittenAt < duration.value(24,'h')`)
- Soft-delete pattern: update that sets `deletedAt = serverTimestamp(), deletedBy = auth.uid` counts as an update (not a delete) — rule applies as written

#### [NEW] `firestore.indexes.json`
- Composite indexes for: `(dateKey, deletedAt)`, `(supplierId, deletedAt, date DESC)`, `(createdBy, deletedAt, serverWrittenAt DESC)`

#### Deploy via Firebase CLI; emulator-test before deploying

---

### Component 12: Documentation (for professor deliverables)

#### [NEW] `docs/Programa1/entrega/02-requerimientos/content.md`
- Functional requirements document in Spanish
- Organized by module: Auth, Purchases, Suppliers, Dashboard, Reports
- Calls out explicit deferred/upcoming features (Settings, admin-managed
  account registration, charts, date-range filter)

#### [NEW] `docs/Programa1/entrega/03-arquitectura/content.md`
- Architecture justification document in Spanish
- MVVM + layered architecture explanation, package structure, Firebase flow, offline behavior
- **Security section:** server-side Firestore rules as authorization SoT (cites ADR-0002)

#### [NEW] `docs/Programa1/entrega/04-modelo-de-datos/content.md`
- Firestore data model documentation
- Collection schemas, relationships, indexes
- Soft-delete and denormalization conventions

#### [NEW] `docs/Programa1/entrega/06-glosario/content.md`
- Canonical Spanish glossary for domain terms, roles, timestamps, money, and authorization policy

#### [NEW] `docs/Programa1/entrega/07-manual-de-usuario/content.md`
- User-facing manual for login, dashboard, purchase capture, supplier administration, and reports

#### [NEW] `docs/Programa1/entrega/08-plan-de-pruebas/content.md`
- Manual test plan mapped to the implemented flows and Firestore security cases

---

## Package Structure

```
com.example.mangos/
├── MangosApp.kt                    # Hilt Application
├── MainActivity.kt                 # Entry point
├── data/
│   ├── model/
│   │   ├── User.kt
│   │   ├── Supplier.kt
│   │   ├── Purchase.kt
│   │   └── UserRole.kt
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── SupplierRepository.kt
│   │   └── PurchaseRepository.kt
│   ├── util/
│   │   ├── MoneyFormatter.kt
│   │   └── DateKey.kt
│   └── di/
│       └── AppModule.kt
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── purchases/
│   │   ├── AddEditPurchaseScreen.kt
│   │   ├── AddEditPurchaseViewModel.kt
│   │   ├── PurchaseHistoryScreen.kt
│   │   └── PurchaseHistoryViewModel.kt
│   ├── suppliers/
│   │   ├── SupplierListScreen.kt
│   │   ├── SupplierListViewModel.kt
│   │   ├── AddEditSupplierScreen.kt
│   │   └── AddEditSupplierViewModel.kt
│   ├── reports/
│   │   ├── ReportsScreen.kt
│   │   └── ReportsViewModel.kt
│   ├── navigation/
│   │   ├── MangosNavGraph.kt
│   │   ├── Screen.kt
│   │   └── BottomNavBar.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
```

---

## Execution Order

| Phase | Components | Estimated Time |
|-------|-----------|---------------|
| 1 | Project Setup + Dependencies + Firebase config + `google-services.json` | ~1.5 hours |
| 2 | Theme & Design System | ~30 min |
| 3 | Data Layer (Models + Repositories + Money/DateKey utils + DI) | ~3 hours |
| 4 | Login Screen | ~1 hour |
| 5 | Navigation + MainActivity + MangosApp (Hilt) | ~1 hour |
| 6 | Dashboard Screen (with `dateKey` query + pending-sync indicator) | ~2 hours |
| 7 | Purchase Screens (Add/Edit with UNREGISTERED + History with supplier filter, soft-delete) | ~3 hours |
| 8 | Supplier Screens (CRUD, deactivate-not-delete) | ~1.5 hours |
| 9 | Reports Screen (text-only — today's tons, today's spend, top-5 suppliers) | ~1 hour |
| 10 | **Firestore Security Rules + indexes + emulator test** | ~2 hours |
| 11 | Documentation (`entrega/` requirements, architecture, data model, glossary, user manual, test plan) | ~2 hours |
| 12 | Testing & Polish (incl. offline scenarios, edit-window) | ~2 hours |
| **Total** | | **~20.5 hours** |

Estimate excludes inevitable Gradle/Hilt/Firebase setup snags (budget 2–4h slack across the 5 days). Settings screen and public Register screen cut per WARNING block above; authenticated Admin-only account management is upcoming work. Charts (Vico) cut from Reports; can be added back at the end if everything else is green.

---

## Verification Plan

### Automated / Build Tests
- `./gradlew assembleDebug` — verify the project compiles
- `./gradlew lint` — check for code quality issues
- Manual functional testing on emulator

### Manual Verification
1. **Auth flow:** Admin creates accounts in Console → Login as Operator → Login as Admin → Logout via Dashboard menu
2. **Purchase flow:** Add purchase with price → Add purchase without price → Add purchase against UNREGISTERED with freeform note → Verify all appear in history → Edit → Soft-delete (verify row hidden from queries but doc remains in Firestore)
3. **24h edit window:** Operator edits own purchase within 24h ✓; edits >24h-old purchase ✗ (security rule denies)
4. **Supplier flow:** (as Admin) Add supplier → Edit → Deactivate → Verify deactivated supplier no longer in dock dropdown but historical purchases still resolve its name
5. **Reports:** Add several purchases (some without price) → Verify tonnage totals all rows; spend total excludes price-less and shows "(N sin precio)" hint
6. **Offline at-the-dock:** Airplane mode → Add 3 purchases → Verify pending-sync badge on dashboard → Turn connectivity back on → Verify sync, badges clear, `serverWrittenAt` populated
7. **Role check (server-side):** Login as Operator → Verify Suppliers tab hidden → Attempt to write to `suppliers/*` via direct Firestore call (or emulator UI) → Verify rule **denies**
8. **Self-promotion attempt:** Operator attempts to write `role: "admin"` to own user doc → Verify rule **denies**
9. **Timezone:** Add purchase at 23:30 Mexico City time → Verify `dateKey` matches local date, not UTC date

### Distribution
- Generate signed APK for submission
- Document installation steps
