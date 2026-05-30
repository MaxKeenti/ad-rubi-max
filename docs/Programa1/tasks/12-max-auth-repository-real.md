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

## Verificación (2026-05-29, emulador Pixel 10, API 37)

Probado manualmente contra el proyecto Firebase real con tres cuentas de
prueba: admin, operator y "huérfana" (existe en Auth pero sin
`users/{uid}` en Firestore).

| Caso | Resultado |
|---|---|
| Operador inicia sesión con credenciales válidas | ✓ Aterriza en Dashboard sin pestaña Proveedores |
| Cerrar sesión desde overflow del Dashboard | ✓ `currentUser` emite `null` y la UI regresa a Login |
| Admin inicia sesión | ✓ Aterriza en Dashboard con pestaña Proveedores visible |
| Cuenta huérfana (Auth sin doc Firestore) | ✓ Permanece en Login con mensaje "Esta cuenta no tiene perfil…"; `auth.signOut()` se invoca para no dejar sesión colgada |
| Contraseña incorrecta | ✓ Mensaje de error desplegado |
| Caché persistente + modo avión + relanzamiento | ✓ Dashboard hidrata desde caché sin crashear |

### Hallazgos / decisiones de implementación

- **Mensajes de FirebaseAuth no vienen en español.** Se agregó
  `authErrorMessage(FirebaseAuthException)` en
  `AuthRepositoryFirestoreImpl` que mapea los códigos comunes
  (`ERROR_INVALID_CREDENTIAL`, `ERROR_INVALID_LOGIN_CREDENTIALS`,
  `ERROR_USER_NOT_FOUND`, `ERROR_WRONG_PASSWORD`, `ERROR_USER_DISABLED`,
  `ERROR_TOO_MANY_REQUESTS`, `ERROR_NETWORK_REQUEST_FAILED`,
  `ERROR_INVALID_EMAIL`) a strings en español. Los códigos
  desconocidos caen al `localizedMessage` original como fallback.
- **`ERROR_INVALID_CREDENTIAL` vs `_LOGIN_CREDENTIALS`.** Firebase
  consolida usuario-no-existe y password-incorrecto en un solo código
  (varía por versión del SDK) para evitar enumeración de cuentas; el
  mensaje en español es genérico "Correo o contraseña incorrectos."
- **Ruido en Logcat `RecaptchaAction(action=signInWithPassword)`.** Es
  el log interno de Firebase cuando intenta primero la ruta sin
  reCAPTCHA y luego cae al fallback. No afecta el comportamiento ni el
  mensaje al usuario; ignorar.
- **Carga del perfil falla por permisos.** Si `loadUserDoc` lanza
  `FirebaseFirestoreException.PERMISSION_DENIED`, `signIn` cierra
  sesión y devuelve un mensaje que incluye el `uid` y apunta a revisar
  las reglas — sirve para depurar despliegues de `firestore.rules`
  incompletos.
- **`currentUser` self-heals.** Si el listener de auth recibe un user
  cuyo doc Firestore no se puede leer (perfil borrado, reglas rotas),
  el flow llama `auth.signOut()` para evitar quedar en un estado
  inválido (sesión sin perfil).
