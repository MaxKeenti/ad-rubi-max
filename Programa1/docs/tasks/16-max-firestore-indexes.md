# Task 16 — Max — Firestore composite indexes

**Owner:** Max
**Estimated effort:** ~30 min
**Prerequisites:** `14-max-purchase-repository-real` (to know which queries exist)
**Day:** 4

## Goal

Declare the composite indexes Firestore needs for the multi-field queries
used by Purchase and Supplier observers. Without these, queries fail at
runtime with "this query requires an index" — the Firebase Console
generates a one-click create link, but for reproducibility we declare
them in code.

## Inputs

- `docs/entrega/04-modelo-de-datos/content.md` § 3 (indexes table)
- Actual queries in `data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt`

## Outputs

- `firestore.indexes.json` at the program root:

  ```json
  {
    "indexes": [
      {
        "collectionGroup": "purchases",
        "queryScope": "COLLECTION",
        "fields": [
          { "fieldPath": "dateKey", "order": "ASCENDING" },
          { "fieldPath": "deletedAt", "order": "ASCENDING" },
          { "fieldPath": "enteredAt", "order": "DESCENDING" }
        ]
      },
      {
        "collectionGroup": "purchases",
        "queryScope": "COLLECTION",
        "fields": [
          { "fieldPath": "supplierId", "order": "ASCENDING" },
          { "fieldPath": "deletedAt", "order": "ASCENDING" },
          { "fieldPath": "date", "order": "DESCENDING" }
        ]
      },
      {
        "collectionGroup": "purchases",
        "queryScope": "COLLECTION",
        "fields": [
          { "fieldPath": "createdBy", "order": "ASCENDING" },
          { "fieldPath": "deletedAt", "order": "ASCENDING" },
          { "fieldPath": "serverWrittenAt", "order": "DESCENDING" }
        ]
      }
    ],
    "fieldOverrides": []
  }
  ```

- Deploy via:
  ```sh
  firebase deploy --only firestore:indexes
  ```

- Wait ~5 minutes after deploy for indexes to build before testing
  composite queries.

## Acceptance criteria

- [ ] `firebase deploy --only firestore:indexes` succeeds.
- [ ] In the Firebase Console → Firestore → Indexes tab, all three composite indexes show "Enabled" status.
- [ ] Running the app and triggering each query type (Dashboard today, History by supplier, edit-window check) produces no "requires an index" errors.

## Pitfalls / notes

- **Indexes build takes ~minutes**, not instant. Don't trigger queries until they're "Enabled."
- **`deletedAt` in ASCENDING order is correct** even though all live values are `null`. Firestore treats null as a sortable value for index purposes.
- **If you add a new query later, add the corresponding index here, not via Console only** — Console-created indexes don't sync back into this file, and the next deploy will drop them.
- **Single-field indexes are auto-created** by Firestore for every field; you don't declare them.
