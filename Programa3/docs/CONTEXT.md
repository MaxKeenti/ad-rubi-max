# BacheWatch — Domain Context

Glossary of canonical terms for the Programa3 pothole reporting app.
Implementation details belong elsewhere; this file is language only.

---

## Reporte

An **observation**: a record that a citizen, standing at a pothole,
photographed it and captured the device's GPS fix at that moment. It is
*not* an incident, case, or work order — there is no status lifecycle
(`reportado → reparado`), no merging, and no authority actor in v1.

- **Duplicates are expected and welcome.** Two people reporting the same
  pothole produce two Reportes; duplicate density *is* the incidence
  signal the heatmap renders. There is no dedup.
- **Mutable?** Never. The payload is machine-captured (photo, GPS); the
  fix for a bad report is delete-and-retake, not edit.
- **Deletable?** Soft-delete only, by the creator, within 24 h of
  `serverWrittenAt`. See "Soft-delete & moderation."

### Required vs optional fields

Photo, coordinates, and `tipo` are required — photo/coordinates are
captured, while `tipo` is selected from one-tap chips (`bache` default,
`otro` for other street issues). `severidad` (leve / moderado / severo,
one-tap chips, no default) and `descripcion` (≤200 chars) are optional
and nullable; they never block save. Same philosophy as Programa1's
optional price. Older documents without `tipo` are interpreted as
`bache`.

### Where and when it gets created

**At the problem location, live.** The flow is: open app → system camera
captures the photo → the GPS fix taken at that same visit is attached →
save.
There is no gallery picker and no pin-drop; a coordinate on a Reporte was
always measured by a device standing at the pothole. "I remember a
problem on my street" is explicitly unsupported.

**Reporting requires connectivity.** The target context is urban streets
with mobile data. A failed submit keeps the composed report on screen
with a retry button; nothing persists a process death. This is an
explicit assumption, not an oversight — a pothole has a natural retry
(it isn't going anywhere), and the offline machinery Programa1 needed at
the dock would buy nothing here. See `implementation_plan.md`.

### One timestamp

`serverWrittenAt` (`FieldValue.serverTimestamp()`) is the only clock.
Programa1 needed three timestamps because of back-dating and offline
queueing; live capture removed back-dating and online-required removed
the client/server gap, so the model collapses to one field. It is
authoritative for the 24 h delete window and fine for relative display
("hace 2 horas"). The photo's EXIF preserves true capture time as a free
forensic backstop. There is no `dateKey` — no day-bucket queries exist
in this app's scope.

### Geo fields

Every Reporte carries `lat`, `lng`, `accuracyMeters`, and a `geohash`
string computed at write time. All four are written once and never
updated — reports don't move. `geohash` exists so the map can query by
viewport (see ADR-0002). `accuracyMeters` records the GPS fix quality;
poor fixes are **flagged data, not rejected data** (soft accuracy gate),
and the field doubles as the evidence base for a future pin-adjust
feature if fixes prove routinely worse than ~25 m.

## Confirmación

A citizen saying **"sigue ahí"** on someone else's (or their own)
Reporte. Stored at `reportes/{reporteId}/confirmaciones/{uid}` — the uid
*is* the document ID, so one-confirm-per-user is a structural guarantee,
not a query-time check. A denormalized `confirmCount` on the Reporte is
incremented in the same batch.

A Confirmación is a historical fact ("I observed it was still there on
this date") and is therefore **never deleted or undone**. The UI renders
the button disabled once your own confirmación exists.

## Ciudadano (identity)

There are no accounts. Every user is signed in silently via **Firebase
Anonymous Auth** on first launch; the resulting `uid` is what security
rules constrain and what `createdBy` records. See ADR-0001.

Consequences to keep in mind:
- Identity dies with the installation. "Mis reportes" as a durable
  concept does not exist; the delete-own affordance lives in the detail
  sheet instead.
- Abuse control is per-installation, not per-person.
- Upgrade path: Firebase supports linking an anonymous account to
  email/Google later without losing the uid.

## Location privacy stance

The app requests `ACCESS_FINE_LOCATION` and uses a **single-shot**
`getCurrentLocation(PRIORITY_HIGH_ACCURACY)` at the moment of capture.
Never continuous tracking, never background location. A coarse-only
grant (Android 12+) is treated as denial for reporting — a ~2 km
coordinate is worthless for a pothole.

A user who denies location gets a fully functional **read-only** app:
map, heatmap, recientes, and detail all work with zero permissions; only
the "Reportar" button gates on location.

## Photo evidence

Captured via the system camera intent (no `CAMERA` permission, no
gallery access, no `READ_MEDIA_IMAGES`). Compressed client-side to max
1600 px / JPEG q≈80 (~300–500 KB) before upload to Firebase Storage at
`reportes/{reporteId}.jpg`.

**Write ordering invariant:** the photo is uploaded *first*; the
Firestore doc is written only on upload success. A crash between phases
leaves an orphaned photo (invisible, harmless, cleanable) — never a
report pointing at a missing image.

## Zonas con mayor incidencia

Rendered as a heatmap toggle over the map, fed by observation density
and **weighted by severity** (leve 1, moderado 2, severo 3, unset 1.5).
The zones view therefore answers "where are the *dangerous* zones," not
just "where are the dots." Markers (severity-colored, tappable) are the
default view; the heatmap is a chip away.

## Soft-delete & moderation

- `deletedAt: Timestamp?`, `deletedBy: String?` — Programa1's pattern
  verbatim. All queries filter `deletedAt == null`. The photo stays in
  Storage. Hard-delete is never used by clients.
- Only the creator may soft-delete, and only within **24 h of
  `serverWrittenAt`** (covers the blurry-accident case without letting
  anonymous uids strip-mine old data).
- **There is no in-app moderation.** Abuse handling (offensive photos,
  spam) is manual via Firebase Console, where the project owner can
  delete any doc or Storage object. At real civic scale this becomes a
  moderation queue + abuse-report flow — named v2, out of scope.

## Authorization policy

Server-side, via Firestore + Storage Security Rules (Programa1's
ADR-0002 stance carried forward):

- `reportes/*`: read = signed-in; create = signed-in with
  `createdBy == auth.uid` and a valid shape; update = **only** the
  soft-delete fields by the creator within 24 h, **or** exactly
  `confirmCount = old + 1` (the confirmar batch); no client hard-delete.
- `reportes/{id}/confirmaciones/{uid}`: create = signed-in with doc ID
  == `auth.uid`; no update, no delete.
- Storage `reportes/{reporteId}.jpg`: read = signed-in; write =
  signed-in, content-type `image/jpeg`, size-capped.

Known residual gap, accepted and documented: a hostile client could
increment `confirmCount` without creating its confirmación doc. Closing
it requires `getAfter()` batch introspection in rules; the stakes
(inflating a pothole's count) don't justify it in v1.
