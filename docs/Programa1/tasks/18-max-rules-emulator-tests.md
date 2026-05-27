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
