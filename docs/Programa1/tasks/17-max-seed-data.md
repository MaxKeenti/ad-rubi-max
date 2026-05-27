# Task 17 — Max — Seed data (UNREGISTERED + first admin + sample suppliers)

**Owner:** Max
**Estimated effort:** ~30 min
**Prerequisites:** `00-max-firebase-project` (project exists), `15-max-firestore-rules` (rules allow the writes if you're authenticated as the admin)
**Day:** 4

## Goal

Make sure the production Firestore has the documents required for the app
to function: the reserved `UNREGISTERED` supplier, the first admin user
doc, and 2–3 realistic sample suppliers for the demo.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2.2, § 2.1
- `docs/Programa1/entrega/06-glosario/content.md` § "Proveedor no registrado"

## Outputs

Two paths to choose from — pick whichever is faster for you:

### Path A: Firebase Console (manual clicks)

1. Open Firestore Console.
2. Create `suppliers/UNREGISTERED` doc with:
   ```
   name: "Proveedor no registrado"
   phone: ""
   email: ""
   location: ""
   mangoVariety: ""
   isActive: true
   createdAt: <serverTimestamp via "set field" UI>
   createdBy: "system"
   ```
3. Create `users/{your-uid}` if not already done in `00-max-firebase-project` — confirm role is `"admin"`.
4. Create 3 sample suppliers via the Console:
   - `Hernández y Hermanos` (Veracruz, Ataulfo, active)
   - `Mangos del Pacífico` (Nayarit, Manila, active)
   - `Frutas Selectas SA` (Oaxaca, Tommy Atkins, **inactive** — useful for demoing the deactivation filter)

### Path B: Node.js seed script

Create `scripts/seed.mjs`:

```js
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
initializeApp({ credential: cert("path/to/service-account-key.json") });
const db = getFirestore();

await db.doc("suppliers/UNREGISTERED").set({
  name: "Proveedor no registrado",
  phone: "", email: "", location: "", mangoVariety: "",
  isActive: true,
  createdAt: FieldValue.serverTimestamp(),
  createdBy: "system",
});

// ... 3 more sample suppliers ...
```

Run with `node scripts/seed.mjs`. **Add `scripts/service-account-key.json` to `.gitignore` — that file is a real secret.**

## Acceptance criteria

- [ ] `suppliers/UNREGISTERED` exists.
- [ ] At least 3 active suppliers exist in the demo project.
- [ ] At least 1 inactive supplier exists (for CP-08 demo).
- [ ] The admin user can log in and see all suppliers.
- [ ] The app's `ensureUnregisteredExists()` from task 13 runs idempotently and doesn't duplicate.

## Pitfalls / notes

- **`UNREGISTERED` is a special case** — its `name` is shown literally in the AddEditPurchase dropdown. Don't translate it accidentally.
- **The service-account key is a secret** — different from `google-services.json`. Real secret. Never commit. `.gitignore` it.
- **Path A is faster for one-off demo seeding.** Path B is reproducible. Pick A unless you'll re-seed multiple times.
- **Sample data should be realistic** — Mexican supplier names, real states (Veracruz, Oaxaca, Sinaloa, Nayarit), real mango varieties (Ataulfo, Manila, Tommy Atkins, Kent, Haden).
