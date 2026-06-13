# Task 11 — Max — Firestore + Storage security rules

**Owner:** Max
**Estimated effort:** ~2.5 h
**Prerequisites:** 00, 06
**Day:** 3 (Jun 13)
**Blocks:** 12
**Never cut** — server-side authz is the rubric.

## Goal

Encode the §3 policy server-side: shape-validated creates, the
exactly-confirmCount-plus-one update, the 24 h creator soft-delete,
immutable confirmaciones, content-typed size-capped Storage writes.

## Inputs

- `docs/implementation_plan.md` §3 (rules summary — the spec)
- `docs/CONTEXT.md` — "Authorization policy"
- Precedent: `Programa1/firestore.rules` (soft-delete window pattern)

## Outputs

- `firebase.json`, `firestore.rules`, `firestore.indexes.json` (empty
  indexes expected), `storage.rules` at `Programa3/` root; deployed
  with `firebase deploy --only firestore:rules,storage`.
- `reportes` **create**: signed-in; `createdBy == auth.uid`;
  `confirmCount == 0`; `deletedAt/deletedBy` absent; lat/lng/geohash/
  accuracyMeters/fotoPath/fotoUrl present with correct types;
  `severidad in ['leve','moderado','severo']` or absent; descripcion
  ≤200 or absent; `serverWrittenAt == request.time`.
- **update**, exactly one of:
  (a) creator, within `request.time < resource.data.serverWrittenAt + duration.value(24, 'h')`,
  diff keys == {deletedAt, deletedBy}, `deletedBy == auth.uid`;
  (b) diff keys == {confirmCount}, new == old + 1.
- **delete**: false. **read**: signed-in.
- `confirmaciones/{uid}` create: `uid == auth.uid`,
  `confirmedAt == request.time`; update/delete false; read signed-in.
- Storage `reportes/{id}.jpg`: read signed-in; write signed-in,
  `contentType == 'image/jpeg'`, `size < 1.5MB`; delete false.

## Acceptance criteria

- [ ] All app flows still work after deploy (the honest smoke test).
- [ ] Console-side manual probe: editing `lat` on an existing doc via
      a client SDK call is rejected.
- [ ] The accepted residual gap (increment without confirmación doc)
      is **documented in a comment in the rules file** with the
      `getAfter()` hardening named.

## Pitfalls / notes

- `affectedKeys()` / `diff()` is the right tool for "exactly these
  fields changed" — hand-rolled field comparisons rot.
- `serverWrittenAt == request.time` only holds because the client uses
  `FieldValue.serverTimestamp()` — if a rules test sends a literal
  date it must equal `request.time`.
- Storage rules can't read Firestore (no cross-service `get` on Spark
  patterns we used in P1) — the photo write is authorized by auth +
  shape only; an orphan upload is acceptable (Q8).
