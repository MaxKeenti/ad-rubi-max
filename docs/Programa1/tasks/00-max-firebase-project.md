# Task 00 — Max — Firebase project setup

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** none
**Day:** 0
**Blocks:** `00-melanie-bootstrap-android` (until `google-services.json` exists)

## Goal

Create the Firebase project, configure Authentication + Firestore, generate
`google-services.json`, and hand it off to Melanie so she can build the
Android project against real Firebase deps.

## Inputs

- `docs/Programa1/entrega/04-modelo-de-datos/content.md` § 2 (collections to create)
- `docs/Programa1/entrega/06-glosario/content.md` (domain terms — useful when naming things in the Console)
- `docs/Programa1/adr/0002-server-side-authz.md` (won't deploy rules yet, but read to know what you're building toward)

## Outputs

- Firebase project created at `console.firebase.google.com`. Suggested ID: `mangos-usa-app-2026` (or whatever's available).
- Region: **`nam5`** (US central multi-region) is fine; `southamerica-east1` or `us-central1` if you want a specific single-region. **Whichever you pick is permanent** — Firestore region cannot be changed later.
- **Authentication** enabled with email/password provider only.
- **Firestore** created in **Native mode** (not Datastore mode).
- Empty collections — no rules deployed yet (default open rules are fine for dev; we lock them down on day 3).
- `Programa1/app/google-services.json` — downloaded from the Firebase Console, committed to the repo.
- Initial admin user created in **Authentication** tab with your email + a strong password; document the password in your password manager.
- Manual `users/{your-uid}` document created in Firestore with `{ email, displayName: "Max", role: "admin", accountCreatedAt: <serverTimestamp> }`.

## Acceptance criteria

- [ ] `google-services.json` committed and pushed.
- [ ] Melanie can pull the branch and the file lives at `Programa1/app/google-services.json`.
- [ ] You can log in at `console.firebase.google.com` and see the project + the one admin user + the one users doc.
- [ ] Firestore is in **Native mode** (verify the badge in the Console).

## Pitfalls / notes

- **Datastore mode is irreversible.** Double-check you picked Native.
- **Don't deploy security rules yet.** The default "test mode" rules (open
  for 30 days) are fine while we're building. Strict rules ship in task 15.
- **Don't commit your admin password.** Only `google-services.json` goes in
  git.
- The `google-services.json` file contains an API key. This is **not a
  secret** — it identifies the project to the SDK. Real authorization lives
  in security rules + Authentication. Safe to commit.
- After this task, you can move on to `00-max-draft-interfaces`. Melanie is
  in parallel on `00-melanie-bootstrap-android`.
