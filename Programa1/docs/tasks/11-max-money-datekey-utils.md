# Task 11 — Max — Money formatter + DateKey util (+ unit tests)

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** `00-melanie-bootstrap-android` complete (need the project to compile)
**Day:** 1

## Goal

Two pure utility files that the rest of the app depends on for correct
money and day-bucket semantics. Unit-tested so we know the math is right
before any UI consumes them.

## Inputs

- `docs/entrega/06-glosario/content.md` § "Money" and § "`dateKey` — el campo bucket-por-día"
- `docs/entrega/04-modelo-de-datos/content.md` § 2.3 (Purchase fields) and § 5 (Money: `Long centavos`)

## Outputs

### `app/src/main/java/com/example/mangos/data/util/MoneyFormatter.kt`

```kotlin
package com.example.mangos.data.util

import java.text.NumberFormat
import java.util.Locale

/** Formats Long centavos as "$1,234.56 MXN". Null → "—". */
fun Long?.centavosToMxnString(): String { ... }

/** Parses "1234.56" or "1,234.56" → 123456L. Empty/blank → null. Invalid → throws IllegalArgumentException. */
fun String.parseMxnToCentavos(): Long? { ... }
```

Locale: `Locale("es", "MX")`. Use `NumberFormat.getCurrencyInstance(...)` for output if it gives `$1,234.56`; otherwise format manually.

### `app/src/main/java/com/example/mangos/data/util/DateKey.kt`

```kotlin
package com.example.mangos.data.util

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId

private val MX_ZONE = ZoneId.of("America/Mexico_City")

/** Returns "YYYY-MM-DD" in Mexico City local time. */
fun Timestamp.toDateKey(zone: ZoneId = MX_ZONE): String { ... }

/** Returns today's dateKey. Convenience for queries. */
fun todayDateKey(zone: ZoneId = MX_ZONE): String { ... }
```

### `app/src/test/java/com/example/mangos/data/util/MoneyFormatterTest.kt`

JUnit 4 tests covering:

- `123456L.centavosToMxnString() == "$1,234.56 MXN"` (or whatever the canonical form is — assert what you produce, but pick a form and stick to it)
- `null.centavosToMxnString() == "—"`
- `"1234.56".parseMxnToCentavos() == 123456L`
- `"1,234.56".parseMxnToCentavos() == 123456L`
- `"".parseMxnToCentavos() == null`
- `"abc".parseMxnToCentavos()` throws

### `app/src/test/java/com/example/mangos/data/util/DateKeyTest.kt`

JUnit 4 tests covering:

- A Timestamp constructed at `2026-05-26T23:30:00-06:00` produces `"2026-05-26"` (Mexico City local).
- A Timestamp at `2026-05-27T00:30:00-06:00` (same instant, the next local day) produces `"2026-05-27"`.
- A Timestamp at the same instant as the first one but read in `UTC` would produce `"2026-05-27"` — verify the function does NOT do that with the default zone.
- `todayDateKey()` returns today's date in `"YYYY-MM-DD"` format.

After writing the fakes' temporary inline `dateKey` computation in
`00-max-fakes-and-hilt-module`, **come back to `FakePurchaseRepository`
and replace the TODO** with `purchase.date.toDateKey()`.

## Acceptance criteria

- [ ] `./gradlew test` runs and all utility tests pass.
- [ ] `./gradlew assembleDebug` still passes.
- [ ] `FakePurchaseRepository`'s `dateKey` TODO is replaced.
- [ ] CP-11 (Timezone of `dateKey`) acceptance is achievable — the math is correct, will be verified manually later when the real PurchaseRepository ships.

## Pitfalls / notes

- **Don't use `java.util.Date`** — it's a footgun. `java.time` only.
- **Mexico abolished DST in 2022** (except border zones). `ZoneId.of("America/Mexico_City")` handles this correctly; verify with a test if you're paranoid.
- **Long vs Int overflow** for centavos — a purchase of 100 tons at $999,999.99/ton is `~10^13 centavos`. Comfortably inside Long range. Don't switch to Int.
- **NumberFormat with Locale es_MX** outputs `"$1,234.56"` with non-breaking space, which can confuse string equality assertions. Either match what NumberFormat produces or format manually. Pick one and stick to it.
