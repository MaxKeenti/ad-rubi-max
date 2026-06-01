# Work Division — Max + Melanie

> **Current status (2026-06-01):** this is now a historical planning
> document. The contract-first split was executed, real Firebase bindings
> replaced fakes, user management shipped, and the final implementation
> snapshot lives in `docs/Programa1/implementation_plan.md`.

**Decided:** 2026-05-26 (grilling session continuation)
**Team:** González Calzada Maximiliano (Firebase access), Sosa Montoya Melanie Rubí (Android Studio)
**Budget:** ~5 days, ~20 effective hours per person
**Branches:** `max`, `rubi` → merge into `main`

This document captures the work-division plan. The detailed task files live in
`docs/Programa1/tasks/`. The "why" behind decisions lives here.

---

## 1. Coordination model: contract-first parallel

The split was chosen as **(B) Contract-first parallel** over alternatives:

- (A) Pure separation — would push all Kotlin onto Melanie (+~30% load).
- (C) Mixed by-component — same outcome but less symmetrical.

**How it works.** Repository *interfaces* (Kotlin `interface` declarations
under `data/repository/`) are the seam between Max's Firebase work and
Melanie's Android UI work. Once those interfaces are frozen (end of day 0),
both work independently against them:

- Melanie writes UI screens consuming the interfaces, backed by Max's
  trivial in-memory **fake** implementations.
- Max writes the real **Firestore-backed** implementations, security rules,
  indexes, and seed data.

The two streams meet incrementally during days 2–3 when the Hilt module
flips bindings from Fake → Real, one repository at a time.

## 2. Day-0 bootstrap sequence

The first day is **partially serial** because nothing can compile until the
project skeleton exists, and nothing can be tested in parallel until the
fakes exist. Sequence:

```
Max (h0–h2)      Melanie (h0–h4)
─────────────    ─────────────
Firebase project
google-services.json
        │
        ▼  (Melanie unblocked)
                 Add deps, plugins, MangosApp,
                 package structure, first build
Draft interfaces ─── 30-min sync ─── Melanie reviews & accepts
        │
Write fake impls
+ initial AppModule
        │
        ▼
End of day 0: Melanie can pull and start UI work against fakes.
```

Concrete day-0 tasks live in `tasks/00-day0-*.md`.

## 3. Component ownership

| # | Item | Owner | Est. h |
|---|---|---|---|
| 2 | Theme & design system | Melanie | 0.5 |
| 3a | `data/model/*.kt` (pure data classes) | Melanie | 0.5 |
| 3b | `data/util/MoneyFormatter.kt`, `DateKey.kt` + tests | Max | 1 |
| 3c | `data/repository/*Impl.kt` (real Firestore-backed) | Max | 2.5 |
| 3d | `data/di/AppModule.kt` (Hilt bindings; controls Fake/Real flip) | Max | 0.5 |
| 4 | Login screen + VM | Melanie | 1 |
| 5 | Navigation + MainActivity + Hilt entry point | Melanie | 1 |
| 6 | Dashboard screen + VM | Melanie | 2 |
| 7 | AddEdit Purchase + Purchase History + VMs | Melanie | 3 |
| 8 | Supplier List + AddEdit Supplier + VMs | Melanie | 1.5 |
| 9 | Reports screen + VM | Melanie | 1 |
| 10a | `firestore.rules` + emulator test suite | Max | 2 |
| 10b | `firestore.indexes.json` + deploy | Max | 0.5 |
| 10c | Seed data (UNREGISTERED, first admin, ~3 suppliers) | Max | 0.5 |
| 11 | Finalize Spanish entrega (screenshots, reflexión, mermaid→SVG) | Max + screenshots from Melanie | 1.5 |
| 12 | Integration testing (all 12 CP cases) | Both | 2 |
| — | Signed APK build + README + submission packaging | Melanie | 0.5 |

**Totals:** Melanie ~11 h, Max ~8.5 h, both together ~2 h.

The imbalance (~2.5 h) is partially offset by the design-lead hours Max
already invested in the original grilling session.

## 4. Integration strategy

### 4.1 Branches

**(W1) Person-named branches** — keep `max` and `rubi`, merge to `main` at
the end of each work session. No feature branches (over-engineering for a
5-day project).

### 4.2 Fake → Real cutover (incremental)

**(X2) Incremental swap**, in this sequence:

1. **AuthRepository real** — end of day 2. Lowest complexity; unblocks
   real-login + role-based-nav testing.
2. **SupplierRepository real** — midday day 3. Smaller surface; unlocks
   Supplier admin screens.
3. **PurchaseRepository real** — end of day 3 / morning of day 4. Largest
   surface (offline, three timestamps, dateKey, UNREGISTERED). Most
   likely to surface bugs; done when Melanie's screens are stable enough
   to serve as integration tests.

This sequencing reserves day 4 for fixing whatever the purchase-repo
cutover surfaces.

### 4.3 Shared-file ownership (conflict prevention)

| File | Primary owner | If the other needs to edit |
|---|---|---|
| `app/build.gradle.kts` | Melanie | Open an issue or ping Max in chat first |
| `app/src/main/AndroidManifest.xml` | Melanie | Same |
| `MainActivity.kt` | Melanie | Same |
| `MangosApp.kt` | Melanie | Same |
| `ui/navigation/MangosNavGraph.kt` | Melanie | Same |
| `data/di/AppModule.kt` | Max | Same |
| `data/repository/*.kt` (interfaces) | Max — frozen after day-0 review | Re-open the interface discussion as a sync |
| `firestore.rules` | Max | — |
| `firestore.indexes.json` | Max | — |
| `entrega/` Spanish docs | Either, plus `/entrega-sync` | The sync skill closes drift |

## 5. Agent task structure

### 5.1 Format: roll-your-own light task files

**(F2)** — task files live in `docs/Programa1/tasks/`. Each file is a thin
spec pointing at the rich entrega/ docs rather than re-stating content.
GSD was considered and deferred — its STATE.md / executor / verifier
ceremony is designed for hands-off execution, not for the kind of
human-in-the-loop iteration this project needs.

If a single task turns out to need GSD's structure (likely candidate: the
PurchaseRepository real impl with its three timestamps + offline +
UNREGISTERED handling), it gets promoted to a mini-`/gsd-plan-phase`
phase. Lazy escalation.

### 5.2 Granularity: per-feature-slice

**(G2)** — one task per coherent feature unit. Examples:

- "Login screen + LoginViewModel + nav wire-up" = 1 task (not 3).
- "PurchaseRepository real impl + emulator tests" = 1 task.

About 15–19 tasks total covering the entire project.

### 5.3 Trigger model

**(T1)** — each person triggers agents on their own branch. Max runs
data/security agents on `max`; Melanie runs UI agents on `rubi`. Aligns
with (W1).

### 5.4 Definition of Done (uniform across tasks)

Each task's DoD has three layers:

1. **Compiles.** `./gradlew assembleDebug` for Kotlin work; rules emulator
   for `firestore.rules`.
2. **Relevant CP case from `08-plan-de-pruebas` passes** when manually
   tested.
3. **No regressions** in adjacent CP cases (spot-check).

Each task file lists the specific CP case(s) it must satisfy.

## 6. Risks and contingencies

| Risk | Likelihood | Mitigation |
|---|---|---|
| Firebase project setup snags on day 0 | Medium | Max starts h0 immediately; Melanie can bootstrap Android against an empty `google-services.json` placeholder if needed (build will warn but compile). |
| Hilt + Compose + Firebase BOM version conflict | High | Pin versions on day 0. Use the Firebase BOM, don't pin individual Firebase deps. |
| Real-repo cutover (day 3) breaks Melanie's screens | High | The whole point of (X2) incremental is to catch this early. Auth-first means we discover contract drift on day 2, not day 4. |
| Firestore rules deny something the UI needs | Medium | Max runs the rules emulator test suite (CP-09, CP-10) before deploying. UI client logs Firestore exceptions visibly during dev. |
| 24h Operator edit window unreachable in emulator due to clock issues | Low | The emulator allows clock manipulation. Document the magic in CP-05. |
| Melanie can't take screenshots until app fully works (day 4) | Medium | Screenshots are last 30 min before submission; `07-manual-de-usuario` ships with `<!-- SCREENSHOT -->` placeholders that the agent doesn't worry about until then. |
| Either person finishes early | Low (but desirable) | Pick up: (a) the Vico charts that got cut from Reports, (b) better empty-state UI, (c) more rules emulator coverage. |
| Either person falls behind | Medium | The cut hierarchy from the WARNING block in `implementation_plan.md` is still in effect — Reports can be further trimmed if needed. |

## 7. Communication cadence

- **Sync points:** day-0 interface review (~30 min), end of day 2 (auth
  cutover), midday day 3 (supplier cutover), end of day 3 / morning day 4
  (purchase cutover), day 5 integration session.
- **Async:** push frequently; whoever pulls runs `git log` to see what
  changed. Commit messages should be informative.
- **Blocked?** Push a WIP commit, ping the other person.

## 8. Where to find things

| You want… | Look at… |
|---|---|
| What a domain term means | `docs/Programa1/CONTEXT.md` |
| Why a decision was made | `docs/Programa1/adr/` + `docs/Programa1/grilling-session-2026-05-26.md` |
| The component-level implementation plan | `docs/Programa1/implementation_plan.md` |
| What ships to the professor | `docs/Programa1/entrega/` |
| The detailed work plan and task files | this document + `docs/Programa1/tasks/` |
| To resync the Spanish entrega after editing English sources | `/entrega-sync` |
