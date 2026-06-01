# Mangos USA — Domain Context

Glossary of canonical terms for the Programa1 mango purchase tracking app.
Implementation details belong elsewhere; this file is language only.

---

## Operator

A warehouse worker who records Purchases at the dock. Each Operator has their
own company-issued phone and their own Firebase Auth account; `createdBy` on a
Purchase is the Operator's userId and is treated as accurate attribution.

A shared-tablet model (one device, many humans switching via picker) was
considered and deferred — see ADR-0001 if present.

## Account creation

There is **no self-service registration**. Operators cannot create their own
accounts, and Admins cannot create their own Admin account from inside the app.
The first Admin is still bootstrapped manually in Firebase Console; after that,
an authenticated Admin uses the in-app Users screen to create Operators, create
another Admin, and promote Operators.

This also resolves the "who is the first admin?" bootstrap question:
whoever sets up the Firebase project is the first admin, by virtue of
having Console access.

- An authenticated Admin can create and manage Operator accounts from an
  in-app Admin UI.
- An authenticated Admin can create another Admin account from the same UI.
- Creating another Admin requires the acting Admin to re-enter their login
  credentials as confirmation before the registration is submitted.
- An authenticated Admin can promote an Operator by selecting that Operator
  from the roster. Promotion retires/deactivates the Operator login, creates a
  new Admin login with the same email, then requires both the promoted Operator
  and the acting Admin to re-enter their login credentials before completion.
  The old Operator `uid` remains on historical Purchases for attribution.
- No user can create or promote their own account.
- User creation, retirement, and promotion happen through callable Cloud
  Functions backed by Firebase Admin SDK, not through direct Android writes to
  Firestore.
- Promotion preserves historical attribution: old Purchases keep the old
  Operator `uid` in `createdBy`.

## Authorization policy

Role enforcement is **server-side**, via Firestore Security Rules. Client UI
hides admin-only actions for ergonomics, but the source of truth is the
rules. A user cannot promote themselves to admin by editing their own role
field; rules forbid it. See ADR-0002.

Policy summary:
- `users/{uid}`: active user can read own doc; active admin can read user docs;
  client writes are limited to `displayName`. Creation, role changes, and
  retirement/promotion fields are trusted-backend writes only.
- `suppliers/*`: read = any active signed-in user; write = admin only.
- `purchases/*`: read = any active signed-in user; create = any active signed-in user;
  update/delete = admin OR original creator within 24h of `serverWrittenAt`
  (Operator typo-fix window).

## Supplier

A counterparty the warehouse buys mangoes from. Created and edited by Admins
only — Operators cannot create Suppliers, even at the dock.

### Active vs inactive

`isActive: Boolean` is a soft-deactivate flag. Inactive Suppliers do not
appear in the dock dropdown for new Purchases but still resolve correctly
on historical Purchase rows (their name is denormalized anyway). A Supplier
is never hard-deleted.

### Unregistered supplier (escape hatch)

A reserved Supplier document with `id = "UNREGISTERED"` and name
"Proveedor no registrado" exists at all times. When a truck arrives from a
Supplier not yet in the roster, the Operator records the Purchase against
`UNREGISTERED` and writes the actual counterparty name into
`supplierNoteFreeform` on the Purchase. Admin reconciles these later by
either (a) creating a real Supplier and editing the Purchase's `supplierId`,
or (b) leaving it for low-volume one-offs.

`supplierNoteFreeform` is `null` for normal Purchases and only set when
`supplierId == "UNREGISTERED"`.

## Admin

A user who can manage the Supplier roster and the Operator roster. Admins also
record Purchases. There is no separate "manager" role; admin/operator is the
only role split.

## Purchase

A **receiving event**: a record that a quantity of mangoes physically arrived
from a supplier on a given day. Created when a truck is unloaded and weighed.
Replaces a single sticky note on the warehouse whiteboard.

Once recorded, a Purchase is a historical fact about inventory inflow. It is
**not** a purchase order, commitment, or accounting line item — those are
separate concepts the app does not yet model.

- **Mutable?** Corrections only (e.g. typo in weight). Not freely editable.
- **Deletable?** Soft-delete only. Hard deletion would destroy the audit trail.

### Soft-delete schema

- `deletedAt: Timestamp?` — null on live Purchases; set with client
  `Timestamp.now()` on delete so Firestore's local cache sees a non-null
  value immediately and active-list queries drop the row before the server
  ack. The authoritative audit clock remains `serverWrittenAt`.
- `deletedBy: String?` — userId of the user who deleted (per ADR-0002,
  always an Admin or the original Operator within the 24h window).
- All Purchase queries default to `where deletedAt == null`. Deleted rows
  remain in Firestore for audit; there is no v1 UI for browsing them.
- Hard-delete is never used.

Suppliers use `isActive: Boolean` instead — deactivation is reversible and
hides the Supplier from the dock dropdown without affecting historical
Purchase resolution. Users are not soft-deleted; departing employees are
handled in Firebase Auth (password change, role downgrade).

### Fields with non-obvious meaning

### Three timestamps, three purposes

A Purchase carries three distinct timestamps. Do not collapse them.

- **`date`** — the **received-at** date: when the mangoes physically arrived
  at the warehouse. Set by the Operator, can be back-dated. This is the date
  used by Reports for "tons received on day X."
- **`enteredAt: Timestamp`** — the **client clock** at the moment the
  Operator hit save on the device. Trusted for display only
  ("registrado el viernes 4:00pm"). Survives offline queueing — it reflects
  when the human acted, not when the server received the write.
- **`serverWrittenAt: Timestamp`** — `FieldValue.serverTimestamp()`. The
  moment Firestore actually accepted the write. Authoritative for the
  24h Operator edit window in security rules. Never shown to users.

There is no `createdAt` field. The original plan's `createdAt` was ambiguous
between these three meanings.

A common, expected pattern: `date = Friday`, `enteredAt = Friday 4pm` (offline
on phone), `serverWrittenAt = Monday 9am` (when connectivity returned).

### `dateKey` — the day-bucket field

Every Purchase carries `dateKey: String` formatted `"YYYY-MM-DD"`, computed
from `date` in the Operator's **local timezone** at write time. Day-bucket
queries (Dashboard "today", Reports daily aggregation) use exact-match on
`dateKey`, **never** UTC range math on `date`.

Why a denormalized string instead of computing day-bounds from `date`:
Firestore Timestamps are UTC; Mexico City is UTC−6 year-round (no DST since
2022). A naïve UTC-midnight range query silently includes or excludes
purchases from the wrong calendar day. `dateKey` sidesteps the problem
entirely — the day-bucket is decided once, at the dock, in the timezone
the human cares about.

If `date` is edited, `dateKey` must be recomputed in the same transaction.

### Denormalized name fields are historical, never back-filled

`supplierName` and `createdByName` are denormalized onto every Purchase at
**write time** and **never updated** when the underlying Supplier or User
record later changes. A Purchase is a historical fact — including who it was
attributed to and which supplier name was on the books that day. Renames
affect new Purchases only.

Practical rule: when a Supplier or User is edited, **do not** fan out updates
to existing Purchases.

### Money

All Purchase prices are in **Mexican pesos (MXN)**. There is no per-Purchase
currency override; the corporation is Mexican-operated despite the "USA"
branding, and all suppliers invoice in MXN.

Stored as **`Long centavos`** (e.g. `123456` = `$1,234.56`). Firestore stores
all numbers as IEEE 754 doubles, so cents-as-integer is the only way to get
exact precision and exact aggregation in Reports. UI always formats from
centavos at the boundary; domain code never uses `Double` for money.

Canonical field name: `pricePerTonCentavos: Long`.

### Price is optional

A Purchase can be recorded at the dock **without a price**. Pricing is often
negotiated separately from delivery; blocking truck-unloading on price entry
would defeat the at-the-dock workflow. Admin (or the Operator later) can
backfill `pricePerTonCentavos` afterwards. Reports must tolerate
price-less Purchases (exclude them from spend totals; still count them in
tonnage totals).

### Where and when it gets created

At the dock, on a phone, **as the truck is being unloaded**. The Operator
stands next to the scale, picks the supplier, types the weight, hits save.
Warehouse connectivity is unreliable, so the app must accept Purchase entries
offline and sync them when connectivity returns. A pending-sync state is a
first-class UI concern, not an afterthought.

---
