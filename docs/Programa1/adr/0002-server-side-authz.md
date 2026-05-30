# ADR-0002 — Server-side authorization via Firestore Security Rules

**Status:** Accepted
**Date:** 2026-05-26

## Context

The app has two roles (admin, operator) with different permissions: only
admins manage Suppliers, only admins (or the original Operator within a
short window) can edit/delete Purchases, and no user may promote themselves
to admin.

Firebase Auth + Firestore is an open client-server model: the APK ships
`google-services.json`, and any client with credentials can talk directly
to Firestore. Hiding admin UI in Compose does not authorize anything —
it only de-clutters the screen.

Three options were considered:

- **(I) Firestore Security Rules.** Server-enforced authorization. Roles
  read from `users/{uid}.role`. Rules forbid self-promotion.
- **(II) Client-only enforcement + documented limitation.** Hide admin UI;
  add a "Security Considerations" note acknowledging the gap.
- **(III) Client-only enforcement, undocumented.** Pretend the role split
  is real. Rejected — bad habit, and any reviewer who knows Firebase will
  see through it.

## Decision

Adopt **(I) Firestore Security Rules** as the source of truth for
authorization. Client UI checks roles for ergonomics only.

## Rules (summary)

- `users/{uid}`
  - read: `request.auth.uid == uid` OR caller is admin
  - write: `request.auth.uid == uid` AND `request.resource.data.role == resource.data.role` (cannot change own role); admins can write any user
- `suppliers/{id}`
  - read: any signed-in user
  - write: admin only
- `purchases/{id}`
  - read: any signed-in user
  - create: any signed-in user, AND
    - `createdBy == request.auth.uid`
    - `serverWrittenAt == request.time` (the server clock is the only
      acceptable value — the client cannot forge an older `serverWrittenAt`
      to extend the 24h edit window)
    - `deletedAt == null` AND `deletedBy == null` (cannot create a
      pre-deleted document)
  - update: admin (no restrictions), OR owner within window, defined as:
    - `createdBy == request.auth.uid`
    - `resource.data.deletedAt == null` (cannot edit a soft-deleted purchase)
    - `request.time - resource.data.serverWrittenAt < duration.value(24, 'h')`
    - AND the affected keys are a subset of
      `{supplierId, supplierNoteFreeform, quantityTons,
        pricePerTonCentavos, date, dateKey, deletedAt, deletedBy}` — i.e.
      the owner may revise the operational fields and perform a soft
      delete but cannot rewrite the audit trail (`createdBy`,
      `createdByName`, `enteredAt`, `serverWrittenAt`, `supplierName`)
    - AND if `deletedAt`/`deletedBy` are among the affected keys, then
      `deletedAt == request.time` AND `deletedBy == request.auth.uid` —
      the owner can only soft-delete *as themselves, now*, not stamp the
      deletion with another user's id or backdate it.
  - delete: admin, OR owner within the same 24h / not-already-deleted window

  Note: the 24h window is measured against `serverWrittenAt`, not the
  client-set `enteredAt`. This is deliberate — see CONTEXT.md →
  "Three timestamps, three purposes." Offline-queued writes get a 24h
  edit window starting from when the server accepts the write, not from
  when the Operator hit save on the device.

  The owner-side field whitelist on update closes a gap that would
  otherwise allow an Operator to rewrite the denormalized `supplierName`
  or `createdByName` snapshot, or push `enteredAt` forward to disguise
  when a record was actually captured. Denormalized display fields are
  by policy frozen at write time (see CONTEXT.md → "Denormalization, no
  back-fill"); the rule makes that policy structural rather than
  conventional.

## Consequences

**Positive**
- Authorization is real, not theatrical. The admin/operator split survives
  a hostile client.
- Self-promotion is structurally impossible, not just hidden.
- Defensible in the viva / architecture doc as a concrete server-side
  security mechanism rather than a UI affordance.

**Negative**
- ~1–2 hours of rule-writing and emulator-testing time on a tight budget.
- Rules add a second place where authorization logic lives (the other
  being client-side UI gating). The two must stay consistent.

## Trigger to revisit

- If the role model grows beyond admin/operator (e.g. plant-scoped admins),
  rules will need restructuring and possibly custom claims.
- ~~If offline-write semantics conflict with the 24h Operator edit window~~
  *Resolved 2026-05-26:* the window is anchored on `serverWrittenAt`
  (server timestamp), not on the client-set `enteredAt`. See CONTEXT.md.
