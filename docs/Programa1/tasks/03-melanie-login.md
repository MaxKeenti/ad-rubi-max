# Task 03 — Melanie — Login screen + ViewModel

**Owner:** Melanie
**Estimated effort:** ~1 hour
**Prerequisites:** `02-melanie-data-models`, `01-melanie-theme`, fakes wired
**Day:** 1

## Goal

A Compose login screen with email + password fields and a "Iniciar Sesión"
button. Talks to `AuthRepository` (currently Fake). On success, navigates
to Dashboard. No registration link — admin provisions accounts in the
Firebase Console.

## Inputs

- `docs/Programa1/entrega/02-requerimientos/content.md` § 3.1 (RF-AUTH-01..04)
- `docs/Programa1/entrega/07-manual-de-usuario/content.md` § 2 (user-facing description)
- `Programa1/app/src/main/java/com/example/mangos/data/repository/AuthRepository.kt`

## Outputs

- `ui/auth/LoginScreen.kt` — Composable. Email + password fields, button, loading indicator, error text.
- `ui/auth/LoginViewModel.kt` — `@HiltViewModel`. Exposes `uiState: StateFlow<LoginUiState>` with fields `email`, `password`, `isLoading`, `error: String?`. Methods: `onEmailChange`, `onPasswordChange`, `onSubmit`.

## Acceptance criteria

- [ ] App launches → shows LoginScreen (since `currentUser == null`).
- [ ] Empty field validation: button disabled or shows error.
- [ ] Submit with any credentials (fake accepts anything) → navigates to Dashboard.
- [ ] CP-01 partial (login path) works against the Fake.
- [ ] Branded with mango theme primary color visible on the button.

## Pitfalls / notes

- **No "Crear cuenta" link or screen.** This was explicitly cut (RF-AUTH-04).
- **Spanish UI:** "Correo electrónico", "Contraseña", "Iniciar Sesión", "Olvidé mi contraseña" (link can be a no-op for now or a Toast saying "Pide al admin").
- **Password masking:** `KeyboardType.Password` + `PasswordVisualTransformation`.
- **Don't store password in StateFlow.** Use a `MutableState<String>` inside the screen or accept that it lives in the VM but never logs it.
- **Loading state matters** — wrap the button in `if (isLoading) CircularProgressIndicator() else Button(...)`.
