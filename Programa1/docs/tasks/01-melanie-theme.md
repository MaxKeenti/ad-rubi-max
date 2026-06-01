# Task 01 — Melanie — Theme & design system

**Owner:** Melanie
**Estimated effort:** ~30 min
**Prerequisites:** `00-melanie-bootstrap-android` complete
**Day:** 1

## Goal

Define the mango-inspired Material 3 color palette and typography. This is
purely cosmetic but it sets the tone for every screen that follows, so
ship it before building screens.

## Inputs

- `docs/implementation_plan.md` Component 2 (palette description: rich green primary, warm mango orange secondary, golden yellow tertiary)
- Current files: `app/src/main/java/com/example/mangos/ui/theme/{Color,Theme,Type}.kt` (Compose defaults)

## Outputs

Files to modify:

- `ui/theme/Color.kt` — define the mango palette:
  - Primary (green family): `Color(0xFF2E7D32)` and tonal variations
  - Secondary (orange family): `Color(0xFFFF8F00)` and tonal variations
  - Tertiary (yellow family): `Color(0xFFFFC107)` and tonal variations
  - Surface / background: warm off-whites (e.g. `Color(0xFFFFF8E1)` for light surface tint)
  - Error: standard Material red
- `ui/theme/Theme.kt` — wire the palette into a Material 3 `lightColorScheme(...)`. Keep `dynamicColor` enabled as a fallback for Android 12+.
- `ui/theme/Type.kt` — leave Material 3 default typography. Customize only if there's clear time at the end.

## Acceptance criteria

- [ ] `./gradlew assembleDebug` passes.
- [ ] The bootstrap placeholder screen renders with the mango palette visible (e.g. the placeholder `Text` uses the primary color via `MaterialTheme.colorScheme.primary`).
- [ ] No "color not defined" warnings.

## Pitfalls / notes

- **Don't over-engineer the palette.** Pick three solid colors per role
  (primary/onPrimary/primaryContainer/onPrimaryContainer) and move on.
- **Skip dark theme** for this version unless time permits at the end. The
  professor sees screenshots in light mode anyway.
- This task has no acceptance criteria tied to a CP case — it's a
  precondition for the visual polish of every other screen.
