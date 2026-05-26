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
  - create: any signed-in user, `createdBy == request.auth.uid`
  - update/delete: admin, OR (`createdBy == request.auth.uid` AND `request.time - resource.data.serverWrittenAt < duration.value(24, 'h')`)

  Note: the 24h window is measured against `serverWrittenAt`, not the
  client-set `enteredAt`. This is deliberate — see CONTEXT.md →
  "Three timestamps, three purposes." Offline-queued writes get a 24h
  edit window starting from when the server accepts the write, not from
  when the Operator hit save on the device.

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
