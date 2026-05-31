# Task 19 — Max — Admin user management and operator promotion

**Owner:** Max
**Estimated effort:** ~4-6 hours
**Prerequisites:** `12-max-auth-repository-real`, `15-max-firestore-rules`, `18-max-rules-emulator-tests`
**Day:** post-v1 / next implementation

## Goal

Add authenticated Admin-only user management: Admins can register Operators,
manage the Operator roster, register another Admin with re-authentication, and
promote an Operator to Admin through the documented retirement/recreation flow.

## Inputs

- `docs/Programa1/CONTEXT.md` § "Account creation"
- `docs/Programa1/entrega/02-requerimientos/content.md` § 3.1.1
- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § `users`
- `docs/Programa1/entrega/06-glosario/content.md` § "Creación de cuentas"
- `Programa1/app/src/main/java/com/example/mangos/data/repository/AuthRepository.kt`
- `Programa1/app/src/main/java/com/example/mangos/data/repository/firestore/AuthRepositoryFirestoreImpl.kt`
- `firestore.rules`
- `tests/rules/rules.test.js`

## Required behavior

- No public/self-registration route.
- Operators cannot register themselves.
- Admins cannot register themselves as Admins.
- Authenticated Admins can register new Operators.
- Authenticated Admins can view and manage the Operator roster.
- Authenticated Admins can register another Admin.
- Registering another Admin requires the acting Admin to re-enter their login
  credentials before submission.
- Authenticated Admins can promote an Operator by selecting them from the
  Operator roster.
- Promotion flow:
  1. Admin logs in.
  2. Admin selects the Operator to promote.
  3. System retires/deactivates the Operator login.
  4. System creates a new Admin account using the same email.
  5. Promoted Operator re-enters their login password.
  6. Acting Admin re-enters login credentials as final confirmation.
  7. Historical Purchases keep the old Operator `uid` in `createdBy`.

## Architecture decision

Use a trusted backend entry point for Auth account creation, disabling, and
promotion. Do **not** attempt to implement this only with Android client code
and Firestore writes.

Reason: Firebase Authentication account creation, disabling, and same-email
account recreation require privileged operations. The Android client must not
be trusted to create arbitrary users, assign `role = "admin"`, disable users,
or promote identities. Firestore rules can protect Firestore documents, but
they cannot safely perform Firebase Auth Admin SDK operations.

Recommended implementation: Firebase Cloud Functions callable endpoints backed
by Firebase Admin SDK.

## Outputs

### Backend / trusted API

Create a Functions package if one does not already exist:

- `functions/package.json`
- `functions/src/index.ts`
- `functions/tsconfig.json`

Callable endpoints:

- `createOperatorAccount`
  - Requires caller to be authenticated.
  - Requires caller's `users/{uid}.role == "admin"`.
  - Creates Firebase Auth user.
  - Creates matching `users/{newUid}` Firestore doc with `role = "operator"`.
  - Rejects duplicate email.

- `createAdminAccount`
  - Requires caller to be authenticated Admin.
  - Requires acting Admin password re-auth confirmation before the app calls
    the endpoint.
  - Creates Firebase Auth user.
  - Creates matching `users/{newUid}` Firestore doc with `role = "admin"`.

- `listOperators`
  - Requires caller to be authenticated Admin.
  - Returns users whose role is `operator`, excluding retired/deactivated
    records if that field is added.

- `promoteOperatorToAdmin`
  - Requires caller to be authenticated Admin.
  - Requires the target user to currently be an Operator.
  - Requires proof that the promoted Operator re-entered their password.
  - Requires acting Admin re-auth confirmation before the app calls the
    endpoint.
  - Retires/deactivates the Operator login first.
  - Creates a new Admin account using the same email.
  - Creates `users/{newAdminUid}` with `role = "admin"`.
  - Preserves the old Operator `users/{oldUid}` doc for audit/history, marked
    retired/deactivated if the schema adds that field.
  - Does not rewrite historical Purchases.

### Android data layer

- Add a dedicated repository, e.g. `UserAdminRepository`, rather than bloating
  `AuthRepository`.
- Suggested interface:
  ```kotlin
  interface UserAdminRepository {
      fun observeOperators(): Flow<List<User>>
      suspend fun createOperator(email: String, displayName: String, password: String): Result<Unit>
      suspend fun createAdmin(email: String, displayName: String, password: String, adminPasswordConfirmation: String): Result<Unit>
      suspend fun promoteOperatorToAdmin(operatorUserId: String, operatorPasswordConfirmation: String, adminPasswordConfirmation: String): Result<Unit>
  }
  ```
- Implement with Firebase Functions callable APIs.
- Add Hilt binding in `AppModule.kt`.

### Android UI

- Add Admin-only route, e.g. `users`.
- Add Admin-only navigation entry from the existing Admin surface. Keep it
  hidden for Operators.
- Add Operator roster screen:
  - list Operators
  - create Operator action
  - promote action per Operator
- Add create Operator form.
- Add create Admin form with acting-Admin password confirmation.
- Add promote Operator confirmation flow:
  - selected Operator summary
  - Operator password confirmation field
  - acting Admin email/password confirmation fields
  - explicit final submit button

### Security rules and tests

- Update `firestore.rules` so normal clients still cannot self-promote by
  editing `users/{uid}.role`.
- Ensure only the trusted backend can write privileged user-management fields
  if fields such as `retiredAt`, `disabledAt`, `promotedToUid`, or
  `promotedFromUid` are introduced.
- Extend `tests/rules/rules.test.js` for any new Firestore fields/collections.
- Do not rely on client-side visibility alone for security.

## Acceptance criteria

- [ ] Operator cannot access the Admin user-management UI.
- [ ] Operator cannot create an Operator account.
- [ ] Operator cannot create an Admin account.
- [ ] Operator cannot promote self by writing `role = "admin"` to Firestore.
- [ ] Authenticated Admin can create an Operator account from inside the app.
- [ ] Created Operator can sign in and receives `role = "operator"`.
- [ ] Authenticated Admin can create another Admin only after re-entering Admin credentials.
- [ ] Created Admin can sign in and receives `role = "admin"`.
- [ ] Admin can select an Operator and start promotion.
- [ ] Promotion fails if the Operator password confirmation is wrong.
- [ ] Promotion fails if the acting Admin credential confirmation is wrong.
- [ ] Promotion succeeds when both confirmations are valid.
- [ ] After promotion, the old Operator login is retired/deactivated and cannot continue as a normal Operator.
- [ ] After promotion, a new Admin login exists using the same email.
- [ ] Historical Purchases still show the old Operator `createdBy` uid; no purchase documents are rewritten.
- [ ] Rules emulator tests still pass.

## Pitfalls / notes

- **Same email constraint:** Firebase Auth does not allow two active accounts
  with the same email in the same project. The Operator login must be
  retired/deactivated before creating the Admin login with the same email, or
  the design must be changed to update the existing user role instead of
  recreating the account. This task follows the documented retire-and-recreate
  requirement.
- **Admin SDK required:** user creation, disabling, and privileged role writes
  belong in Firebase Functions/Admin SDK. Do not ship service-account
  credentials in the Android app.
- **Re-authentication:** on Android, use Firebase Auth re-authentication for
  the acting Admin before calling the privileged endpoint. For the promoted
  Operator's password confirmation, the backend must verify the credential in
  a way that does not leak or store passwords.
- **Audit trail:** Purchases use `createdBy` as historical attribution. Do not
  rewrite old Purchases during promotion.
- **Role mutation:** keep the existing self-promotion denial. Promotion is a
  server-side workflow, not an arbitrary client-side update to `users.role`.
- **Schema choice:** if adding fields such as `retiredAt`, `disabledAt`,
  `promotedToUid`, or `promotedFromUid`, update `entrega/04-modelo-de-datos`
  after implementation so the deliverable matches the final schema.
