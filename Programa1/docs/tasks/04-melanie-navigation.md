# Task 04 — Melanie — Navigation graph + bottom bar + Main host

**Owner:** Melanie
**Estimated effort:** ~1 hour
**Prerequisites:** `03-melanie-login` (need at least one screen routed)
**Day:** 1

## Goal

Wire up Jetpack Navigation Compose with a sealed `Screen` route class, a
`MangosNavGraph` that conditionally shows Login vs the authed app, and a
bottom navigation bar shown only when signed in. Admin sees one extra tab.

## Inputs

- `docs/entrega/02-requerimientos/content.md` § 3.4 (Dashboard) and § 3.5 (Reports nav)
- `docs/implementation_plan.md` Component 10
- `app/src/main/java/com/example/mangos/data/repository/AuthRepository.kt` (currentUser StateFlow for conditional routing)

## Outputs

- `ui/navigation/Screen.kt` — sealed class with objects: `Login`, `Dashboard`, `Purchases`, `Suppliers` (admin), `Reports`, `AddEditPurchase(purchaseId: String? = null)`, `AddEditSupplier(supplierId: String? = null)`.
- `ui/navigation/MangosNavGraph.kt` — `@Composable` that takes a `NavHostController`. Observes `AuthRepository.currentUser`; if null shows Login graph, else shows the authed graph with bottom bar.
- `ui/navigation/BottomNavBar.kt` — Composable showing 4 tabs (Operator) or 5 tabs (Admin) with Material icons and Spanish labels: "Inicio", "Compras", "Proveedores", "Reportes".
- `MainActivity.kt` — set content to `MangosTheme { MangosNavGraph(rememberNavController()) }` inside `@AndroidEntryPoint`.

## Acceptance criteria

- [ ] App launches → if not signed in shows LoginScreen; if signed in shows Dashboard with bottom bar.
- [ ] Admin role sees the "Proveedores" tab; Operator does not (filter the items list by `currentUser.role`).
- [ ] Tapping a tab navigates; back-stack behaves sanely (popping doesn't kick you out).
- [ ] CP-12 (role-based tab visibility) passes against Fake.

## Pitfalls / notes

- **Use `NavHost` with `startDestination`** decided by current user state. The simplest pattern: two top-level `NavHost`s, one inside an `if (user == null)` branch.
- **Pass `userId`/`role` through the back-stack arguments**, don't re-read from repo every screen.
- **AddEditPurchase route takes an optional purchaseId** for edit mode. `null` = create.
- **Bottom bar item state:** use `currentBackStackEntryAsState()` to highlight the active tab.
- **Spanish labels:** "Inicio" (Dashboard), "Compras", "Proveedores", "Reportes".
- **Logout entry point** lives in the Dashboard top-bar overflow menu, NOT in the bottom bar.
