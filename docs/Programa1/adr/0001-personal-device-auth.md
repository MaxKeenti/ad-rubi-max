# ADR-0001 — Personal-device auth, not shared-tablet auth

**Status:** Accepted
**Date:** 2026-05-26

## Context

Purchases are recorded **at the dock, as trucks are unloaded** (see CONTEXT.md
→ Purchase). That workflow could plausibly be served by two different identity
models:

- **(i) Personal device per operator.** Every operator has their own
  company-issued phone, signs into Firebase Auth once, stays signed in.
  `createdBy` on a Purchase is the operator's Firebase userId.
- **(ii) Shared warehouse tablet.** One or two devices live at the dock;
  whichever human is on shift uses them. Firebase Auth identifies the
  *device*; a separate `operators` collection identifies the *human*; an
  operator-picker screen runs at app open / after idle, and the picked
  operator's id becomes `createdBy`.

Option (ii) more closely matches how real mango warehouses tend to operate
(workers don't usually have company smartphones), but it requires:

- A second identity collection (`operators`) separate from `users`.
- An operator-picker UI + idle timeout logic.
- An admin UI to manage the operator roster.
- Architecture-doc text explaining the device-vs-human split.

Estimated added cost: 4–6 hours, against a 5-day, two-person budget.

## Decision

Adopt **(i) personal-device auth** for the initial release. `users` is a
single collection; Firebase Auth identity *is* operator identity; `createdBy`
points directly at a Firebase userId.

## Consequences

**Positive**
- Auth model is the Firebase default — no custom session/picker code.
- Saves ~4–6 hours, which is significant on a 5-day budget.
- Forward-compatible with (ii): adding an `operatorId` field later is
  additive, and existing `createdBy` values remain meaningful as the
  device-account id.

**Negative**
- The demo rests on the assumption "every worker has a company phone." That
  is plausible but not universal in this industry. Call it out explicitly in
  the architecture doc rather than glossing over it.
- If the real deployment turns out to be shared-tablet, `createdBy` becomes
  misleading until (ii) is implemented.

## Trigger to revisit

Flip to (ii) if **either** of the following becomes true:

- The deployment target is confirmed to be shared devices (e.g., the client
  says "we'll put two tablets at the dock, workers won't have phones").
- Attribution disputes ("who recorded this load?") arise in practice.

## Alternatives considered

- **(iii) No per-operator attribution at all** — drop `createdBy`, track
  only the device. Rejected: loses the audit trail that justifies the
  whole "replace whiteboard" framing.
