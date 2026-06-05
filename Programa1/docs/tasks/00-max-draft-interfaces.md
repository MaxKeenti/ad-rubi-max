# Task 00 — Max — Draft repository interfaces

**Owner:** Max (drafts), Melanie (reviews)
**Estimated effort:** ~1 hour drafting + 30 min sync
**Prerequisites:** `00-melanie-bootstrap-android` (need the project to compile against the interfaces)
**Day:** 0
**Blocks:** all Melanie UI work, all Max repository implementation work

## Goal

Define the **contract between data and UI layers** as Kotlin `interface`
declarations. Once Melanie signs off in the review sync, these interfaces
are **frozen** for the duration of the project (a re-open requires a new
sync, not a unilateral edit).

## Inputs

- `docs/entrega/04-modelo-de-datos/content.md` (schemas)
- `docs/entrega/06-glosario/content.md` (domain terms)
- `docs/implementation_plan.md` Component 3 (Data Layer — repository methods listed)
- `docs/adr/0002-server-side-authz.md` (what queries the rules allow)

## Outputs

Create the following files. Pure interfaces and data classes only; no
implementations.

### `app/src/main/java/com/example/mangos/data/model/`

- `User.kt`, `Supplier.kt`, `Purchase.kt`, `UserRole.kt` — data classes
  matching the schemas in `04-modelo-de-datos`. Use Kotlin `data class`,
  `Long?` for nullable centavos, `com.google.firebase.Timestamp` for
  Firestore timestamps.

### `app/src/main/java/com/example/mangos/data/repository/`

- `AuthRepository.kt` — interface:

  ```kotlin
  interface AuthRepository {
    val currentUser: StateFlow<User?>            // null = signed out
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut()
    suspend fun getUserRole(userId: String): UserRole
  }
  ```

- `SupplierRepository.kt` — interface:

  ```kotlin
  interface SupplierRepository {
    fun observeActive(): Flow<List<Supplier>>     // for dock dropdown (isActive == true)
    fun observeAll(): Flow<List<Supplier>>        // for admin list (admins only)
    suspend fun getById(id: String): Supplier?
    suspend fun add(supplier: Supplier): Result<String>     // returns new id
    suspend fun update(supplier: Supplier): Result<Unit>
    suspend fun deactivate(id: String): Result<Unit>        // sets isActive = false
    suspend fun ensureUnregisteredExists()                  // seed UNREGISTERED doc if missing
  }
  ```

- `PurchaseRepository.kt` — interface:

  ```kotlin
  interface PurchaseRepository {
    fun observeByDateKey(dateKey: String): Flow<List<Purchase>>
    fun observeBySupplier(supplierId: String, limit: Int = 50): Flow<List<Purchase>>
    fun observeRecent(limit: Int = 5): Flow<List<Purchase>>
    suspend fun getTodaySummary(dateKey: String): TodaySummary    // see PurchaseRepository.TodaySummary
    suspend fun add(purchase: Purchase): Result<String>
    suspend fun update(purchase: Purchase): Result<Unit>
    suspend fun softDelete(id: String, deletedBy: String): Result<Unit>

    data class TodaySummary(
      val totalTons: Double,
      val totalSpendCentavos: Long,
      val purchaseCount: Int,
      val purchasesWithoutPrice: Int,
    )
  }
  ```

> All `Result<T>` methods follow Kotlin's `kotlin.Result` semantics; the
> caller handles `onSuccess` / `onFailure`. All `Flow` methods listen to
> Firestore realtime updates; filtering `deletedAt == null` is the
> repository's responsibility, not the caller's.

## Acceptance criteria

- [ ] All three interface files compile (`./gradlew assembleDebug` still passes — interfaces have no impl, but Hilt module isn't binding them yet, so no error).
- [ ] All four data-class files compile.
- [ ] Melanie has reviewed the interfaces and signed off (in chat, in a commit comment, or in the review sync).
- [ ] After sign-off, this commit is tagged or marked in `work-division.md` so the team knows the contract is frozen.

## Pitfalls / notes

- **Filter `deletedAt == null` inside the repository**, not at the call
  site. If you push that responsibility to the UI, every screen has to
  remember it.
- **Don't expose Firestore types** beyond `Timestamp` — no `DocumentReference`, no `QuerySnapshot`. Models are POKOs.
- **`StateFlow<User?>` for currentUser** lets the UI react to sign-out by collecting in a ViewModel. Don't use `LiveData`; we're on coroutines.
- **`Result<T>` not exceptions** — Firestore can throw; wrap in `runCatching` in implementations.
- **The 30-min review sync with Melanie** is where she catches "wait, how do I do X with this interface?" Run through each method against the entrega/ requirements one at a time.
- After Melanie signs off, immediately start `00-max-fakes-and-hilt-module`.
