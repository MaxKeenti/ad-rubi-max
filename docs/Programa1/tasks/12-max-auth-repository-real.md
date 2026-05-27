# Task 12 — Max — AuthRepository real Firestore implementation

**Owner:** Max
**Estimated effort:** ~1.5 hours
**Prerequisites:** `00-max-fakes-and-hilt-module`, `11-max-money-datekey-utils`
**Day:** 2 (target: end of day 2 cutover)

## Goal

Replace `FakeAuthRepository` with `AuthRepositoryFirestoreImpl` backed by
Firebase Authentication + a `users/{uid}` document read. After this lands,
flip the Hilt binding in `AppModule.kt`. Melanie's UI consumes the new
impl transparently.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.1 (users schema)
- `docs/Programa1/entrega/06-glosario/content.md` § "Política de autorización"
- `Programa1/app/src/main/java/com/example/mangos/data/repository/AuthRepository.kt` (interface)
- `Programa1/app/src/main/java/com/example/mangos/data/model/User.kt`, `UserRole.kt`

## Outputs

- `data/repository/firestore/AuthRepositoryFirestoreImpl.kt`:
  - `@Inject constructor(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore)`
  - `currentUser: StateFlow<User?>` — bridged from `auth.authStateChanges()` callback flow. When a user signs in, also fetch `users/{uid}` to populate role/displayName. Emit `null` on sign out.
  - `signIn(email, password)` — `auth.signInWithEmailAndPassword(...).await()`, then read `users/{uid}` (if missing, sign out and return failure with explanatory error).
  - `signOut()` — `auth.signOut()`.
  - `getUserRole(userId)` — read `users/{userId}.role` and parse via `UserRole.fromFirestoreString`.

- **Modify `data/di/AppModule.kt`:**
  - Add `@Provides @Singleton fun provideFirebaseAuth() = Firebase.auth`
  - Add `@Provides @Singleton fun provideFirestore() = Firebase.firestore.apply { firestoreSettings = ... persistentCacheEnabled }`
  - Flip the binding: `@Binds abstract fun bindAuthRepository(impl: AuthRepositoryFirestoreImpl): AuthRepository`. Comment out the Fake binding but **leave the class** in the repo for now (delete in a later cleanup task).

## Acceptance criteria

- [ ] CP-01 (auth flow) passes against real Firebase using the admin account you created in `00-max-firebase-project`.
- [ ] Operator account (which you'll create for testing) logs in and gets role = `OPERATOR`.
- [ ] Sign out flips `currentUser` to null and the UI returns to Login.
- [ ] A user document missing from Firestore (Auth-only user) is rejected gracefully (signs out + shows error in UI).

## Pitfalls / notes

- **Enable persistent cache before any read/write:**
  ```kotlin
  Firebase.firestore.firestoreSettings = firestoreSettings {
    setLocalCacheSettings(persistentCacheSettings { })
  }
  ```
  Pass that into the Hilt provider. Do this **before** the first Firestore access happens; safest in `MangosApp.onCreate`.
- **Use `kotlinx-coroutines-play-services`** for `.await()` on `Task<T>`. Add dependency: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")`.
- **`callbackFlow` for authStateChanges** is the canonical bridge. Don't poll.
- **The `users/{uid}` read should be a `snapshots()` flow** internally so displayName/role updates propagate. Or, simpler: re-fetch on every auth state change. For v1, simpler is fine.
- **Tell Melanie when this lands.** She switches her testing from "any email/password" to real credentials.
