# Task 15 — Max — Firestore security rules

**Owner:** Max
**Estimated effort:** ~2 hours
**Prerequisites:** `14-max-purchase-repository-real` ideally (so real queries are known)
**Day:** 4

## Goal

Write `firestore.rules` enforcing ADR-0002. Deploy them to the project.
This is the moment authorization becomes real — until now everything was
running on test-mode open rules.

## Inputs

- `docs/adr/0002-server-side-authz.md` (full policy)
- `docs/entrega/03-arquitectura/content.md` § 5 (rules summary)
- `docs/entrega/06-glosario/content.md` § "Política de autorización"

## Outputs

- `firestore.rules` at the program root:

  ```js
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      function isSignedIn() { return request.auth != null; }
      function isAdmin() {
        return isSignedIn() &&
               get(/databases/$(database)/documents/users/$(request.auth.uid))
                 .data.role == "admin";
      }

      match /users/{uid} {
        allow read: if request.auth.uid == uid || isAdmin();
        allow create: if isAdmin();
        allow update: if (request.auth.uid == uid &&
                          request.resource.data.role == resource.data.role)
                       || isAdmin();
        allow delete: if isAdmin();
      }

      match /suppliers/{id} {
        allow read: if isSignedIn();
        allow write: if isAdmin();
      }

      match /purchases/{id} {
        allow read: if isSignedIn();
        allow create: if isSignedIn() &&
                         request.resource.data.createdBy == request.auth.uid;
        allow update, delete:
          if isAdmin()
          || (resource.data.createdBy == request.auth.uid
              && request.time - resource.data.serverWrittenAt
                 < duration.value(24, 'h'));
      }
    }
  }
  ```

- `firebase.json` at the program root (if not present) with:
  ```json
  { "firestore": { "rules": "firestore.rules", "indexes": "firestore.indexes.json" } }
  ```

- Deploy via:
  ```sh
  firebase deploy --only firestore:rules
  ```

## Acceptance criteria

- [ ] Rules deployed to production project.
- [ ] CP-09 (self-promotion denied) verifiable via emulator tests (task 18) or manual Firestore Console attempt.
- [ ] CP-10 (operator can't write suppliers) verifiable.
- [ ] CP-05 (24h edit window) server-side enforced.
- [ ] App still works for normal flows (Melanie can run smoke tests; if anything denies that shouldn't, fix the rules).

## Pitfalls / notes

- **`get()` calls in rules count against your quota.** Each rule that calls `isAdmin()` does one read per evaluation. For a small project this is fine; for production at scale you'd cache the role in a custom claim. Out of scope here.
- **`isAdmin()` requires the caller's user doc to exist.** If a user is in Auth but not Firestore, every admin-gated rule denies. That's the intended behavior — see task 12 graceful sign-out for missing user doc.
- **`request.time - resource.data.serverWrittenAt`** evaluates to a `duration`. Compare with `duration.value(24, 'h')`. The Firestore docs are unclear; test against the emulator (task 18).
- **The `update` rule on purchases must accept the soft-delete write** — verify a write that sets `deletedAt` + `deletedBy` and nothing else passes. If it doesn't, weaken the rule to "allow this specific shape of update."
- **Don't `--force` deploy if firebase CLI warns about open rules.** Read the warnings.
- **Backup the current rules** (test-mode default) before deploy in case you need to roll back: `firebase firestore:rules get`.
