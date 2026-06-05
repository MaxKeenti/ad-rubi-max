# Grilling Session — 2026-05-26

Domain & architecture grilling session over `implementation_plan.md`. Worked
through 10 high-leverage decision branches; produced this log, two ADRs,
and a populated `CONTEXT.md`, plus an in-place rewrite of the plan.

**Participants:** Max González, Claude (Opus 4.7)
**Starting point:** `docs/implementation_plan.md` (original 16h-estimate plan)
**Outputs:**
- `docs/CONTEXT.md` — full domain glossary
- `docs/adr/0001-personal-device-auth.md`
- `docs/adr/0002-server-side-authz.md`
- `docs/implementation_plan.md` — rewritten to reflect decisions
- This log

---

## Q1 — What is a "Purchase," actually?

**Options considered:**
- (A) Receiving event — recorded when a truck arrives at the dock and is weighed
- (B) Purchase order / commitment — a deal struck, mutable until fulfilled
- (C) Accounting line item — invoice/ledger record

**Decision:** (A) Receiving event.

**Consequences agreed:**
1. Purchases are soft-deleted only — hard delete destroys the audit trail
2. `date` (received-at) is distinct from `createdAt` (when the record was entered)

**Why it mattered:** Determined mutability semantics, deletion policy, and the
separation between physical-event time and record-entry time. Everything
downstream — soft-delete schema, the three-timestamp design, denormalization
rules — flows from "Purchase = historical fact."

---

## Q2 — Where and when is a Purchase created?

**Options considered:**
- (α) At the dock, on the phone, while truck is being unloaded — unreliable wifi → offline writes core
- (β) After the fact, from an office — offline is irrelevant

**Decision:** (α) At-the-dock.

**Consequences:**
- Offline writes are first-class, not a freebie
- Supplier list must be cached locally for the dock dropdown
- A pending-sync UI state is required
- ~3-4 hours added to the budget

**Why it mattered:** Made offline a feature requirement, which then forced
the UNREGISTERED-supplier escape hatch (Q5) and surfaced the
`createdAt`-vs-offline-edit-window conflict (Q8).

---

## Q3 — Who is logged in on the phone at the dock?

**Options considered:**
- (i) Personal device per Operator — own phone, signed in once
- (ii) Shared warehouse tablet — operator-picker UI on app open / idle
- (iii) Shared device, no per-operator attribution

**Decision:** (i) Personal device. (ii) was tempting but deferred for
time/scope. Captured in **ADR-0001**.

**Why it mattered:** Determined the auth model and whether `createdBy` is
trustworthy attribution or a lie. (ii) would have required a second
identity collection and ~4-6h of extra work. Forward-compatible: an
`operatorId` field can be added later if the deployment turns out to be
shared-tablet.

---

## Q4 — Currency, money type, and is price even required?

**Sub-questions:**
- (a) Single currency or per-purchase override?
- (b) `Double` for money — accept the precision risk?
- (c) Is price required for recording a Purchase?

**Decision:**
- (a) Single currency, **MXN**, no override
- (b) **No** — use `Long centavos`. Firestore stores all numbers as
  IEEE 754 doubles, so cents-as-integer is the only way to get exact
  precision and exact aggregation in Reports
- (c) **Optional** at the dock. Pricing is negotiated separately;
  blocking truck-unloading on price entry defeats the at-the-dock workflow.
  Reports must tolerate price-less rows.

**Field rename:** `pricePerTon: Double` → `pricePerTonCentavos: Long?`

**Why it mattered:** Removed a silent precision bug from the spec, made
the at-the-dock UX honest (Operator can record arrivals fast), and aligned
the schema with how Firestore actually stores numbers.

---

## Q5 — Denormalized name fields and the "supplier not in list" problem

**Two coupled questions:**
1. What happens when a Supplier renames? Back-fill old purchases?
2. At the dock, an Operator encounters an unknown supplier — what now?

**Decisions:**
1. **Never back-fill.** `supplierName` and `createdByName` are written
   once at write time and remain historical. A Purchase records what was
   true that day. Renames affect new Purchases only.
2. **UNREGISTERED escape hatch.** A reserved Supplier doc with
   `id = "UNREGISTERED"` exists permanently. Operators cannot create new
   Suppliers; instead they record the Purchase against UNREGISTERED and
   write the actual counterparty name into a new field
   `supplierNoteFreeform`. Admin reconciles later.

**Why it mattered:** Resolved the most subtle correctness/UX question in
the schema. The denormalization decision honors the historical-fact
framing from Q1; the UNREGISTERED pattern keeps Operator capability narrow
(no supplier-creation rights) without blocking the dock workflow.

---

## Q6 — Server-side authorization

**Original plan:** role checks in the UI only ("hide nav from Operators").

**Problem surfaced:** Firebase Auth + Firestore is an open client-server
model. Without Firestore Security Rules, any user can:
- Read every document
- Write/delete any document
- Promote themselves to admin by editing their own `users/{uid}.role`

**Options:**
- (I) Add Firestore Security Rules — real server-side authorization
- (II) Skip rules, document the limitation
- (III) Skip rules, don't mention it

**Decision:** (I). Captured in **ADR-0002**.

**Rules summary:**
- `users/{uid}`: read = own or admin; write = own except `role`; admin
  can write any
- `suppliers/{id}`: read = signed-in; write = admin only
- `purchases/{id}`: read = signed-in; create = signed-in with
  `createdBy == auth.uid`; update/delete = admin OR (creator AND
  `request.time - serverWrittenAt < 24h`)

**Why it mattered:** Turned a theatrical role split into a real
architectural feature. Distinguishes the project at the viva — "authz is
server-enforced, here are the rules" rather than hand-waving.

---

## Q7 — Scope cut

**Recognition:** Original 16h estimate was optimistic; the design pass
added Firestore rules, UNREGISTERED pattern, offline UI states, ADRs, and
soft-delete logic. Realistic estimate: 22–26h. Budget: ~30h for 2 people
over 5 days. Effectively zero slack.

**Cuts applied (per recommendation, all accepted):**
- **CUT** Settings screen → logout moves to Dashboard top-bar overflow
- **CUT** Register screen → Admin provisions accounts in Firebase Console
  (also resolves "who is the first admin?" — whoever has Console access is)
- **TRIM** Reports → no Vico charts; text-only metrics
- **TRIM** Purchase History → supplier filter only, no date range
- **KEEP** Edit Purchase / Edit Supplier — required by the 24h
  Operator typo-fix window and UNREGISTERED reconciliation

**Net effect:** estimate brought to ~20.5h.

**Why it mattered:** Forced honesty about the budget and protected the
core value (at-the-dock recording + suppliers + auth + rules) at the
expense of nice-to-haves.

---

## Q8 — The 24h edit window vs offline-queued writes (createdAt conflict)

**Bug found in ADR-0002 as originally drafted:**
The rule `now - createdAt < 24h` collapses two distinct moments:
- When the Operator hit save on the device (Friday 4pm at the dock)
- When the server actually accepted the write (Monday 9am after the phone
  reconnected)

Using a server timestamp makes `createdAt` a lie about when the record
was made; using a client timestamp lets clients lie to the rule.

**Options:**
- (P) Server timestamp only — honest about DB time, but UI shows wrong "when"
- (Q) Client timestamp only — matches reality, but unsafe for authz
- (R) **Two fields, two purposes** — client-set `enteredAt` for display,
  server-set `serverWrittenAt` for the rule. No `createdAt`.

**Decision:** (R). Plus `date` (received-at) as a third, Operator-set
timestamp for the actual delivery day.

**Final timestamp model:**
- `date` — when the truck arrived (Operator-editable, can be back-dated)
- `enteredAt` — client clock when save was hit (display only)
- `serverWrittenAt` — `FieldValue.serverTimestamp()` (authoritative for
  the 24h rule)

**Why it mattered:** Caught a real correctness bug. The fix preserves
both the human timeline ("you recorded this Friday") and the server
timeline ("the row arrived Monday, so the 24h edit window starts Monday")
without conflating them.

---

## Q9 — What does "today" mean? (Timezone for day-bucket queries)

**Problem:** Firestore Timestamps are UTC. The naïve query
"purchases where `date >= startOfToday`" using UTC midnight silently
includes/excludes purchases from the wrong calendar day in Mexico City.

**Options:**
- (α) Hard-code Mexico City timezone for all day-bucket math
- (β) Denormalize a `dateKey: String` ("YYYY-MM-DD") at write time in
  the local zone
- (γ) Per-user-configurable timezone — over-engineered for v1

**Decision:** (β) `dateKey`.

**Properties:**
- Day-bucket queries become exact-string-match (`where dateKey == "2026-05-26"`)
- No timezone math at read time, no compound range query, no index headaches
- Works seamlessly with offline writes (client computes its own dateKey)
- Forward-compatible with multi-region (each warehouse's local day)
- Recomputed in the same transaction whenever `date` is edited

**Why it mattered:** Eliminated a class of silent day-boundary bugs that
would have made the Dashboard's headline metrics intermittently wrong.

---

## Q10 — Soft-delete schema (small)

**Decision:**
- `Purchase`: `deletedAt: Timestamp?`, `deletedBy: String?`. All queries
  filter `deletedAt == null` by default. No hard-delete. No v1 UI for
  browsing deleted rows.
- `Supplier`: `isActive: Boolean` (reversible deactivation; not the same as
  delete). No `deletedAt`.
- `User`: not soft-deleted. Departing employees handled via Firebase Auth
  (password change, role downgrade).

**Why it mattered:** Made the soft-delete agreement from Q1 concrete and
distinguished it from Supplier deactivation, which is reversible and
serves a different purpose.

---

## Themes & meta-observations

- **The "historical fact" frame from Q1 was load-bearing.** Once "a
  Purchase is a historical fact," soft-delete, never-back-fill
  denormalization, and the three-timestamp model all became forced moves.
- **"At-the-dock" surfaced multiple decisions** that wouldn't have come up
  in an office-data-entry workflow: offline writes, UNREGISTERED escape
  hatch, the createdAt-vs-offline conflict, the timezone issue.
- **The plan's biggest weakness was implicit authorization.** Client-only
  role checks looked like a feature on paper but were trivially bypassable.
  Q6 turned that into a real architectural choice with ADR-0002.
- **Scope honesty (Q7) came midway,** after the design had already added
  meaningful work. Better to recognize the budget pressure once the design
  is settled than to keep adding-and-not-cutting.
- **Two ADRs is the right count for this project.** Personal-device auth
  and server-side authz are both hard-to-reverse, surprising-without-context
  decisions with real alternatives. Everything else fits in CONTEXT.md
  (glossary) or the plan (implementation).
