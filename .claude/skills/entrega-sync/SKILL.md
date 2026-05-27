---
name: entrega-sync
description: Keep the Spanish deliverables in docs/Programa1/entrega/ in sync with their canonical English sources (CONTEXT.md, ADRs, implementation_plan.md, grilling-session log). Detect drift between source and translation, propose updates, ask only about translation judgement calls, apply changes, and verify the entrega/ tree still compiles with Typst. Use when the user asks to "sync the entrega", "update the Spanish docs", "check if the entrega is stale", or invokes /entrega-sync.
---

# entrega-sync — keep the Spanish deliverables fresh

The project keeps a **bilingual paper trail by design**:

- **Canonical sources, English** — `docs/Programa1/CONTEXT.md`, `docs/Programa1/adr/*.md`, `docs/Programa1/implementation_plan.md`, `docs/Programa1/grilling-session-*.md`. This is where decisions actually live. Edited freely as the design evolves.
- **Deliverables, Spanish** — `docs/Programa1/entrega/0X-*/content.md` (with Typst wrappers in `main.typ`). Submitted to the professor. Must reflect the current state of the canonical sources.

When the English sources change, the Spanish deliverables drift. This skill catches and closes that drift.

## What to do when invoked

1. **Diagnose drift.** Don't open every file blindly. Use git to scope the work:

   ```sh
   # What changed in the canonical sources since the entrega was last touched?
   git log --since="$(git log -1 --format=%cd docs/Programa1/entrega/)" \
           --name-only --oneline -- docs/Programa1/CONTEXT.md docs/Programa1/adr/ \
                                    docs/Programa1/implementation_plan.md \
                                    'docs/Programa1/grilling-session-*.md'
   ```

   If nothing changed → tell the user the entrega is up to date and stop. Don't invent work.

2. **Map sources → deliverables.** Use the table in "Source-of-truth mapping" below to decide which entrega files are affected by each changed source.

3. **Read both sides.** For each affected pair, read the canonical source and the corresponding section in the Spanish deliverable. Identify three categories of change:
   - **Mechanical** — a sentence was rewritten in English; translate it directly. Don't ask.
   - **Additive** — a new term, rule, ADR, or invariant was introduced. Translate and place. Don't ask unless the placement is ambiguous.
   - **Judgement** — a term has multiple valid Spanish renderings, or a structural change affects multiple deliverables in different ways. **Ask the user.** One question at a time. Recommend a default.

4. **Apply edits.** Use Edit for surgical changes; Write only if a deliverable needs a full rewrite (rare). Keep code identifiers in English (`Long centavos`, `dateKey`, `serverWrittenAt`) — they are field names, not prose.

5. **Verify compilation.** Run:

   ```sh
   cd docs/Programa1/entrega && just build
   ```

   If anything fails to compile, fix it before reporting completion. The most common failures are Typst syntax leaking from a poorly-translated markdown table or a mermaid block that grew too large for a page.

6. **Report.** One paragraph: what was stale, what you changed, anything you flagged for the user's eye, whether the tree still compiles.

## Source-of-truth mapping

| Canonical source (English) | Affected deliverable(s) (Spanish) | Notes |
|---|---|---|
| `CONTEXT.md` → glossary terms | `entrega/06-glosario/content.md` | Direct translation. This is the closest to a 1:1 mirror. |
| `CONTEXT.md` → schema fields | `entrega/04-modelo-de-datos/content.md` | Field names stay English; descriptions translate. |
| `CONTEXT.md` → authorization policy | `entrega/03-arquitectura/content.md` § 5, `entrega/02-requerimientos/content.md` § 4.3 | Cross-referenced; check both. |
| `adr/0001-personal-device-auth.md` | `entrega/05-decisiones-arquitectonicas/content.md` § ADR-0001 | Full translation. Preserve the status header and structure. |
| `adr/0002-server-side-authz.md` | `entrega/05-decisiones-arquitectonicas/content.md` § ADR-0002, `entrega/03-arquitectura/content.md` § 5 | If the rules change, both places update. |
| `implementation_plan.md` → scope cuts | `entrega/01-resumen-ejecutivo/content.md`, `entrega/09-conclusiones/content.md` § 3 | If something gets cut/re-added, both. |
| `implementation_plan.md` → data model | `entrega/04-modelo-de-datos/content.md` | Single source of truth for the schema. |
| `implementation_plan.md` → verification plan | `entrega/08-plan-de-pruebas/content.md` | Cases CP-01..CP-12 mirror the manual-verification list. |
| `grilling-session-*.md` | `entrega/09-conclusiones/content.md` § 2 ("Lo que se aprendió") | New grilling sessions may produce new lessons worth surfacing. |

## Translation policy

- **Prose is Spanish (`lang: "es"`).** Mexican Spanish, neutral register, second-person formal when addressing the user (manual de usuario).
- **Code identifiers stay English.** `pricePerTonCentavos`, `dateKey`, `serverWrittenAt`, `UNREGISTERED`, `isActive`, etc. Inline code spans (`` `like this` ``) and code blocks are not translated.
- **Domain proper nouns stay English.** "Firestore", "Firebase Auth", "Hilt", "Jetpack Compose", "Material 3", "Kotlin", "MVVM", "Clean Architecture", "ADR".
- **Acronyms translate when they have an established Spanish form**, stay English otherwise. "UI" stays "UI". "Base de datos" not "database" in prose.
- **Currency:** `MXN` or `$1,234.56 MXN`. Never `USD`.
- **Dates in prose:** "26 de mayo de 2026". Dates in metadata/headers: ISO `2026-05-26`.

## When to ask the user

Ask (one focused question, recommend a default) when:

- A new ADR appeared in `docs/Programa1/adr/` and you can't tell from context how to integrate it elsewhere.
- A term in CONTEXT.md has been renamed and the rename ripples into multiple deliverables non-uniformly.
- The grilling-session log records a decision that contradicts something already translated in the entrega (the canonical source is authoritative, but the user should confirm before you overwrite).
- The scope changed in `implementation_plan.md` in a way that affects the deferred/cut list in conclusiones.

Don't ask when:

- A sentence was rewritten and the new meaning is unambiguous.
- A new field was added with a clear description.
- An ADR's "Trigger to revisit" was edited.
- Whitespace/formatting changes.

## Things to leave alone

- **Mermaid blocks in deliverables.** Strategy D2: they render as code blocks in the PDF. The user will swap them for embedded SVGs by hand before the final delivery. Don't auto-convert.
- **`<!-- SCREENSHOT: ... -->` placeholders in `07-manual-de-usuario`.** They get filled when the app is implemented.
- **§ 5 "Reflexión del equipo" in `09-conclusiones`.** The team writes this at the very end.
- **The portada-template.typ and shared/style.typ.** They're frozen unless the user explicitly asks to tweak typography.
- **Cover-page metadata** (boletas, profesor name, fecha). These don't drift.

## Verification gotchas

When running `just build`:

- Typst will fetch packages on first run; subsequent runs are fast.
- If a deliverable fails because cmarker chokes on a complex table, the fix is usually to simplify the table in markdown or hand-write that one section in Typst (per the (B-hybrid) lazy escape hatch documented in `docs/Programa1/entrega/README.md`).
- Compilation must pass for **all 9 deliverables** before reporting success. Don't shortcut by only building the one you touched.

## Don't

- Don't translate proactively when nothing changed. Drift-driven, not vibes-driven.
- Don't restructure deliverables to "improve" them. Match the canonical sources; structural changes are a separate user request.
- Don't push commits without explicit user permission. Stage and propose; let the user say "commit".
- Don't open more files than you need to diagnose the specific drift. The entrega tree is large; read selectively.
