# Task 12 — Max — Rules emulator test suite

**Owner:** Max
**Estimated effort:** ~3 h
**Prerequisites:** 11
**Day:** 4 (Jun 14)
**Never cut** — "the exactly-plus-one and 24 h-window rules are wrong
until tested" (grilling Q16).

## Goal

`@firebase/rules-unit-testing` suite run via
`firebase emulators:exec --only firestore,storage "npm test"` —
Programa1's pattern (`Programa1/tests/`), extended to Storage.

## Inputs

- `Programa1/tests/` (harness layout, package.json, how P1 wired
  emulators:exec)
- `docs/implementation_plan.md` §6 (the named cases)

## Outputs

`Programa3/tests/rules/` — package.json + `reportes.rules.test.mjs`,
`storage.rules.test.mjs`. Required cases (each asserts the *denial*,
plus one happy-path control per group):

1. create with `createdBy != auth.uid` → denied
2. create with `confirmCount != 0`, bad `severidad`, descripcion >200,
   missing geo fields → denied (shape violations)
3. confirmación with doc id != auth.uid → denied
4. `confirmCount` increment by 2 → denied; by 1 → allowed
5. confirmación update/delete → denied (immutable)
6. soft-delete by non-creator → denied
7. soft-delete at `serverWrittenAt + 25h` → denied; at +1 h → allowed
   (manipulate the doc's timestamp via `withSecurityRulesDisabled`)
8. soft-delete touching any field beyond deletedAt/deletedBy → denied
9. unauthenticated read → denied
10. Storage: non-JPEG contentType and >1.5 MB → denied

## Acceptance criteria

- [ ] Suite green locally via one command (documented in README):
      `firebase emulators:exec --only firestore,storage "npm --prefix tests/rules test"`.
- [ ] Each §3 policy line maps to ≥1 test (traceability list in the
      test file header — the entrega's plan-de-pruebas cites it).
- [ ] Suite is deterministic (fixed timestamps, no sleeps).

## Pitfalls / notes

- 24 h-window tests: write the doc with rules disabled and a
  *backdated* `serverWrittenAt`, then attempt the delete as the
  creator — don't try to mock `request.time`.
- Storage emulator needs `storage.rules` referenced in `firebase.json`
  or it silently tests allow-all.
- Melanie re-runs this exact command as the release gate (task 17) —
  keep it one-command and machine-independent (pin emulator versions
  in `firebase.json`).
