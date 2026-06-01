---
name: task-run
description: Execute a task file from docs/tasks/. Given a task ID (e.g. "03", "12", "00-melanie-bootstrap-android"), the skill reads the task spec, loads the linked context documents, produces the listed Outputs by writing code, and verifies the Acceptance criteria before reporting completion. Use when the user says "run task X", "execute task X", "do task X", "/task-run X", or asks an agent to implement a specific task from the work-division plan.
---

# task-run — execute a task file from docs/tasks/

The task system in `docs/tasks/` packages units of work as
self-contained `.md` files. Each file declares Inputs (context to read),
Outputs (files to produce), and Acceptance criteria (how to verify
"done"). This skill executes one such task end-to-end.

## How to invoke

The user typically calls it as:

```
/task-run 03
/task-run 03-melanie-login
/task-run docs/tasks/03-melanie-login.md
```

Resolve the input to a concrete task file:

- A number → match against `NN-*.md` files in `docs/tasks/`. If
  multiple match (e.g. both day-0 files start with `00`), ask which one.
- A partial slug → fuzzy match.
- An explicit path → use as-is.

If the file doesn't exist, list available task files and ask the user
which one they meant. Don't guess.

## Execution flow

### Phase 1 — Load context

1. **Read the task file completely.** It is short by design (~50–100 lines).
2. **Read every doc named in the Inputs section.** Don't skim — the
   acceptance criteria assume you've absorbed them. Specifically:
   - The relevant section of `docs/entrega/*/content.md`
     (Spanish formal docs)
   - The relevant section of `docs/CONTEXT.md`
   - Any ADRs (`docs/adr/*.md`)
   - The interface files (`app/src/main/java/com/example/mangos/data/repository/*.kt`)
   - Existing code that will be modified
3. **Cross-reference with `work-division.md`** if the task touches a
   shared file owned by the other person — flag if the user is running a
   task outside their lane.

### Phase 2 — Plan before writing

Before producing any file, state:

- What files you'll create / modify
- What the key design choices are (where the task spec has wiggle room)
- Any prerequisites that look incomplete (e.g. the interface file is empty)

Stop and ask if anything is ambiguous. The user is in the loop. Don't
silently guess on judgment calls.

### Phase 3 — Produce outputs

Write the files in the order they appear in the task's Outputs section.
For each:

- Use Edit for surgical changes to existing files.
- Use Write for new files.
- Follow Kotlin conventions: package declarations, imports ordered, idiomatic Compose patterns, coroutines + Flow over callbacks.
- Spanish for user-facing strings; English for identifiers and comments.
- Match the project's style from existing files (look at the empty Compose scaffold's package declarations as the canonical style).

### Phase 4 — Verify acceptance criteria

Walk the Acceptance criteria checklist. For each:

- **"Compiles" / `./gradlew assembleDebug`**: run it. If the user has Gradle wrapper at `gradlew`, use that.
- **"CP-XX passes"**: read the corresponding CP case in `entrega/08-plan-de-pruebas/content.md` and walk through whether the code as written would satisfy it. Don't claim a CP passes if you can't trace from code to assertion.
- **"No regressions"**: spot-check the obvious adjacent flows.

If something fails, fix it before reporting completion. Don't ship a
task with red criteria.

### Phase 5 — Report

Output a concise summary:

```
Task <ID> completed.

Files created:
  - path/A
  - path/B
Files modified:
  - path/C

Acceptance:
  [✓] Compiles
  [✓] CP-02 traced through (logic matches assertion)
  [⚠] CP-07 (offline) — verified against Fake; full verification requires the real PurchaseRepository (task 14)

Notes:
  - <anything the user should know>
```

Be honest about what's verified vs. what's "I think it works but I
can't fully test from here."

## Branch awareness

Before starting, run `git branch --show-current`. The task files are
owner-tagged (`max` or `melanie`):

- Running a `max` task on the `rubi` branch — flag it. Confirm with the
  user; the work-division plan is they each work on their own branch.
- Running a `melanie` task on the `max` branch — same.
- Running on `main` directly — usually fine for day-0 setup, but flag for
  later tasks.

## Commit policy

**Do not commit automatically.** Stage the changes (`git add`) so the
user sees a clean diff, then let them invoke commit themselves. Reason:
each commit becomes part of the project's grade-evidence; the human
chooses the commit message.

If the user explicitly says "commit", follow the project's commit-message
conventions visible in `git log` (concise summary line, optional body,
`Co-Authored-By` if appropriate).

## Things to leave alone

- **Don't edit task files themselves** during execution. If the task spec
  is wrong, surface that as a question; don't silently rewrite it.
- **Don't edit the entrega/ Spanish docs as a side effect** — that's
  `/entrega-sync`'s job, triggered separately.
- **Don't edit CONTEXT.md or ADRs** during a task. Those are
  grilling-session territory; if executing a task surfaces a domain
  question, stop and ask the user.

## When to refuse

If the task file's Prerequisites are not satisfied (e.g. trying to run
`03-melanie-login` before `02-melanie-data-models` has produced the data
classes), refuse with a clear "this task depends on X, which doesn't
appear complete." Don't try to bootstrap the prerequisite as a side
effect.
