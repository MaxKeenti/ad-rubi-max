# ADR-0001 — Firebase Anonymous Auth as the identity model

**Status:** Accepted (grilling session, 2026-06-11, Q2)
**Deciders:** Max González

## Context

BacheWatch's brief never mentions accounts, but two design facts demand
*some* identity: (1) the "confirmar" action must be one-per-user-per-report,
and (2) Firestore/Storage Security Rules — server-enforced authorization is
a standing convention from Programa1's ADR-0002 — need an `auth.uid` to
constrain writes. The brief's core phrase is *"reportar baches de manera
sencilla"*; friction before the first report is the primary failure mode of
civic-reporting apps.

## Options considered

1. **Firebase Anonymous Auth** — silent sign-in on first launch; stable
   `uid` per installation; zero UI.
2. **Email/password accounts** — full login/registration (already built
   once in Programa1). Durable identity, but two screens of friction that
   contradict "sencillo," for a rubric that grades geo/maps/storage/camera,
   not auth.
3. **Google Sign-In** — one tap, durable, but SHA-1/OAuth config overhead
   and a third-party dependency for marginal gain.
4. **No auth** — open Firestore/Storage writes. Rejected outright; it is
   exactly the hole ADR-0002 (Programa1) exists to close.

## Decision

**Option 1: Anonymous Auth.** `FirebaseAuth.signInAnonymously()` ensured at
app start by `SesionAnonima`; every Reporte carries `createdBy = auth.uid`;
confirmaciones are keyed by uid.

## Consequences

- Zero-friction first report; no login/registration screens exist.
- Rules remain as strong as Programa1's: every write is attributable to a
  uid and constrained by it (create-own, delete-own-within-24h,
  one-confirm-per-user).
- **Identity dies with the installation.** Uninstall/reinstall = new uid;
  prior reports become un-deletable by their author (acceptable: reports
  are immutable observations anyway). "Mis reportes" as a durable feature
  is out of scope.
- Abuse control is per-installation, not per-person. A motivated abuser
  can reinstall for a fresh uid; the backstop is manual Firebase Console
  moderation (see CONTEXT.md).
- Forward-compatible: Firebase account linking can upgrade an anonymous
  user to email/Google later **without changing the uid**, so no data
  migration is ever needed to add real accounts.
