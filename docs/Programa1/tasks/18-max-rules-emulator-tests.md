# Task 18 — Max — Firestore rules emulator test suite

**Owner:** Max
**Estimated effort:** ~1.5 hours
**Prerequisites:** `15-max-firestore-rules`
**Day:** 4

## Goal

Automated tests that verify the rules behave as expected. Without these
the security claim in ADR-0002 is hand-waved.

## Inputs

- `firestore.rules` (the rules you wrote in task 15)
- `docs/Programa1/entrega/08-plan-de-pruebas/content.md` CP-09, CP-10 (the manual cases that this automates)

## Outputs

- `firebase.json` — add emulator configuration:
  ```json
  {
    "emulators": {
      "firestore": { "port": 8080 },
      "auth": { "port": 9099 },
      "ui": { "enabled": true }
    }
  }
  ```
- `tests/rules/package.json` with `@firebase/rules-unit-testing` dev dep.
- `tests/rules/rules.test.js` — at minimum these cases:

  ```js
  import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
  import fs from "fs";

  const env = await initializeTestEnvironment({
    projectId: "mangos-test",
    firestore: { rules: fs.readFileSync("firestore.rules", "utf8") },
  });

  // helpers: db(uid, role) returns an authed firestore client + seeds the user doc

  describe("users", () => {
    test("operator cannot promote self to admin", async () => {
      // seed users/op1 with role=operator
      // op1 attempts to update own doc with role=admin
      // expect: assertFails
    });
    test("operator can update own displayName", async () => { ... });
  });

  describe("suppliers", () => {
    test("operator cannot write suppliers", async () => { ... });
    test("admin can write suppliers", async () => { ... });
  });

  describe("purchases", () => {
    test("operator can create own purchase", async () => { ... });
    test("operator cannot create purchase with createdBy = otherUid", async () => { ... });
    test("operator can edit own purchase within 24h", async () => { ... });
    test("operator cannot edit own purchase after 24h", async () => {
      // seed a purchase with serverWrittenAt = 25 hours ago
      // operator update attempt expects: assertFails
    });
    test("admin can edit any purchase anytime", async () => { ... });
  });
  ```

- Run with:
  ```sh
  firebase emulators:exec --only firestore,auth "npm --prefix tests/rules test"
  ```

## Acceptance criteria

- [ ] All test cases pass.
- [ ] CI-style report: green check or clear failure.
- [ ] At least 8 distinct test cases (one per branch of the rules).
- [ ] The "after 24h" test uses a real backdated `serverWrittenAt` value to verify the duration math.

## Pitfalls / notes

- **`@firebase/rules-unit-testing` is the official harness.** Don't try to test by hitting production Firestore.
- **Bypassing rules for setup**: use `env.withSecurityRulesDisabled(async ctx => { ... })` to seed test data without writing through the rules.
- **Backdated timestamps:** in tests, write `serverWrittenAt: Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000)`. The emulator accepts arbitrary Timestamp values.
- **This test suite is also documentation** for what the rules do. Future-Max in 6 months will read it to remember what's allowed.
- **CI integration:** out of scope here, but the same `firebase emulators:exec` command would be the CI entry point if we had CI.

## Verificación (2026-05-30)

```sh
firebase emulators:exec --project mangos-test --only firestore,auth \
  "npm --prefix tests/rules test"
```

termina con `Script exited successfully (code 0)` y el reporter de
`node --test` reporta:

```
ℹ tests 15
ℹ pass 15
ℹ fail 0
```

15 casos cubren las 4 ramas de `/users`, las 4 ramas de `/suppliers` y
las 7 ramas relevantes de `/purchases` (8 mínimos exigidos; entregamos
casi el doble).

### Hallazgos / decisiones de implementación

- **Runner: `node --test` integrado** en vez de jest/mocha. Razón: Node
  24 trae el test runner estable, evita dependencias extra y mantiene
  `tests/rules/package.json` con sólo `@firebase/rules-unit-testing` +
  `firebase` (el SDK web cliente que el harness usa por debajo). El
  reporter `spec` da la salida verde-check / cruz que pide el criterio
  de aceptación.
- **`firebase emulators:exec --only firestore,auth`** arranca ambos
  emuladores aunque las reglas se evalúan con contextos
  pre-autenticados de `rules-unit-testing` (no se pega contra el
  emulador de Auth). Levantar Auth no estorba y coincide con el
  comando documentado en el spec; además deja la UI lista (`localhost:4000`)
  por si Future-Max quiere poke manual.
- **Reglas leídas con path relativo al archivo de tests**:
  `new URL("../../firestore.rules", import.meta.url)`. Esto hace al
  comando portable: corre igual desde `tests/rules/` (vía `npm test`)
  que desde la raíz (vía `firebase emulators:exec`).
- **Backdating real para la ventana de 24h**:
  `Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000)` para el caso
  "after 24h" y `- 23 * 3600 * 1000` para "within 24h". El emulador
  acepta el `Timestamp` arbitrario y la rama
  `request.time - resource.data.serverWrittenAt < duration.value(24, 'h')`
  se evalúa contra ese valor — sin esto el test no probaría la
  aritmética de duración.
- **Seed vía `env.withSecurityRulesDisabled`** para `users/{uid}` y
  para purchases backdated. Sin esto, sembrar al operador requeriría
  pasar por `isAdmin()` (que aún no existe en el test). El helper
  `seedUser(uid, role)` materializa el doc directo y luego
  `env.authenticatedContext(uid).firestore()` pega contra las reglas.
- **`assertFails` produce ruido esperado en stderr**: el SDK
  imprime `@firebase/firestore: GrpcConnection ... PERMISSION_DENIED`
  como log, no como error. El reporter sigue contando el test como
  pass porque la promesa sí rechazó. No es regresión.
- **Casos extra más allá del spec** (todos en branches distintos de las
  reglas, cada uno gana un check):
  - `operator cannot edit suppliers` — separa update de create, la
    regla `allow write` cubre ambas pero el test las distingue.
  - `admin can edit suppliers` — espejo del anterior.
  - `signed in users can read purchases` y `anonymous users cannot
    read purchases` — cubren `allow read: if isSignedIn()` en
    `/purchases`.
  - `operator can soft delete own purchase within 24h` — verifica
    que la regla `purchaseOwnerAllowedChanges` acepta el shape
    `{deletedAt, deletedBy}` y que `purchaseOwnerAllowedChanges`
    exige `deletedBy == request.auth.uid`.
  - `operator cannot hard delete purchase after 24h` — cubre la rama
    `allow delete` post-ventana.
- **CP-09 (auto-promoción denegada)** trazado por `users › operator
  cannot promote self to admin`. CP-10 (escritura no autorizada en
  `suppliers`) trazado por `suppliers › operator cannot write
  suppliers` + `operator cannot edit suppliers`. CP-05 (ventana 24h)
  servidor-lado trazado por los cuatro tests de purchases que tocan
  `serverWrittenAt`.
- **`.gitignore` ampliado** con `node_modules/`,
  `firebase-debug.log`, `firestore-debug.log`, `ui-debug.log`. Los logs
  los escribe `firebase emulators:exec` en la raíz del repo en cada
  corrida; sin ignore terminarían en el árbol de trabajo.
- **`firebase.json` emulador**: puertos `8080` (firestore), `9099`
  (auth), UI habilitada. Coincide con el spec textualmente.
