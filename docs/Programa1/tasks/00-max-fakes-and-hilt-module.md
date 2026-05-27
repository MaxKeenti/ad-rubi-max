# Task 00 — Max — Fake repositories + initial Hilt module

**Owner:** Max
**Estimated effort:** ~1.5 hours
**Prerequisites:** `00-max-draft-interfaces` complete and signed off
**Day:** 0
**Blocks:** all Melanie UI work from day 1

## Goal

Implement trivial in-memory **fake** repositories so Melanie can build UI
in parallel while Max writes the real Firestore-backed implementations.
Wire them into Hilt so the app actually runs end-to-end with fake data.

## Inputs

- `Programa1/app/src/main/java/com/example/mangos/data/repository/*.kt` (the interfaces from `00-max-draft-interfaces`)
- `docs/Programa1/entrega/06-glosario/content.md` (for seed data — what an UNREGISTERED supplier looks like, what realistic suppliers look like)

## Outputs

### `Programa1/app/src/main/java/com/example/mangos/data/repository/fake/`

- `FakeAuthRepository.kt` — single hardcoded admin user: `User("max-uid", "max@mangos.test", "Max", UserRole.ADMIN, …)`. `signIn` accepts any email/password and returns this user. `signOut` flips the StateFlow to null.
- `FakeSupplierRepository.kt` — `MutableList<Supplier>` seeded with:
  - `Supplier("UNREGISTERED", "Proveedor no registrado", …, isActive = true, …)`
  - `Supplier("sup1", "Hernández y Hermanos", "555-1111", "h@h.com", "Veracruz", "Ataulfo", isActive = true, …)`
  - `Supplier("sup2", "Mangos del Pacífico", "555-2222", "p@m.com", "Nayarit", "Manila", isActive = true, …)`
  - `Supplier("sup3", "Frutas Selectas SA", "555-3333", "f@s.com", "Oaxaca", "Tommy Atkins", isActive = false, …)` (one inactive, so the dock dropdown filtering can be tested)
- `FakePurchaseRepository.kt` — `MutableList<Purchase>` initially empty. Implements all observe/add/update/softDelete methods using a `MutableStateFlow<List<Purchase>>`. `add` generates a UUID id, populates `enteredAt = now()`, `serverWrittenAt = now()` (the fake doesn't distinguish), `dateKey` via `DateKey.toDateKey(...)` — but wait: `DateKey` doesn't exist yet (it's a later task). Put a TODO comment and use `LocalDate.now(ZoneId.of("America/Mexico_City")).toString()` inline; replace after task 11.

### `Programa1/app/src/main/java/com/example/mangos/data/di/AppModule.kt`

Hilt module that binds the interfaces to fakes:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
  @Binds @Singleton
  abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

  @Binds @Singleton
  abstract fun bindSupplierRepository(impl: FakeSupplierRepository): SupplierRepository

  @Binds @Singleton
  abstract fun bindPurchaseRepository(impl: FakePurchaseRepository): PurchaseRepository
}
```

When the real impls land (tasks 12, 13, 14), this file is the **one place**
that flips Fake → Real, one line at a time.

## Acceptance criteria

- [ ] `./gradlew assembleDebug` passes.
- [ ] App launches; if Melanie has added a temporary `Text(supplier.name)` collecting `observeActive()`, it shows the three seed suppliers.
- [ ] Adding a purchase via Melanie's eventual AddPurchase screen appears in the fake's list immediately and emits through the Flow.
- [ ] Logging out via `FakeAuthRepository.signOut()` flips `currentUser` to null.

## Pitfalls / notes

- **No persistence.** The fakes lose all state on app restart. That's
  intentional — they're scaffolding.
- **Don't simulate network delay or errors.** Keep them trivial. If
  Melanie wants to test loading states, she can do it locally with a
  temporary `delay(500)` in her ViewModel.
- **Hilt's `@Binds` requires `abstract class`** — not `object`. Easy to
  trip on.
- **Inject `@Inject constructor()` on each Fake** so Hilt can instantiate
  them.
- **Push and notify Melanie** the moment this lands — she's blocked.
- The `DateKey` TODO is fine; task 11 will replace the inline call.
