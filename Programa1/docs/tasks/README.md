# Tasks — agent-executable work units

> **Current status (2026-06-01):** these files are historical execution
> specs. The shipped implementation is summarized in
> `../implementation_plan.md`; verification results live in
> `../guia-pruebas-melanie.md`.

Each `.md` file in this directory is one **agent-executable task**. The
naming convention encodes order and owner:

```
NN-<owner>-<short-name>.md
```

- `NN` — sequence prefix. `00-*` files are day-0 bootstrap (must finish
  first). `01-*` onward can be tackled in any order *within an owner's
  lane*, respecting the prerequisites declared inside each file.
- `<owner>` — `max` or `melanie`.
- `<short-name>` — kebab-case task summary.

## How to use a task file from a Claude session

```
@docs/tasks/03-melanie-login.md

Please execute this task. Stop and ask if anything is ambiguous.
```

The agent reads the task file, follows the **Inputs** links to load
context (CONTEXT.md, the relevant entrega/ sections, repository
interfaces), produces the **Outputs**, and verifies the **Acceptance
criteria**.

## Task file structure

Every task file has the same sections:

- **Owner / Estimated effort / Prerequisites / Day** — top-of-file
  metadata.
- **Goal** — one sentence on what this task accomplishes.
- **Inputs (read these first)** — links to entrega/ docs, ADRs,
  interfaces, and any existing code to modify.
- **Outputs** — exact files the agent should create or modify.
- **Acceptance criteria** — checklist tied to specific CP cases from
  `entrega/08-plan-de-pruebas/content.md`.
- **Pitfalls / notes** — things easy to get wrong, things the entrega
  docs don't fully cover.

## Order of execution

Day-0 tasks run **sequentially** because they have hard prerequisites:

```
00-max-firebase-project     →  (provides google-services.json)
                                       │
                                       ▼
                              00-melanie-bootstrap-android
                                       │
                                       ▼
                              00-max-draft-interfaces
                                       │  (30-min review sync)
                                       ▼
                              00-max-fakes-and-hilt-module
```

From day 1 onward both owners' lanes run in parallel. Within a lane,
tasks have soft prerequisites declared in their header.

## Status tracking

This is **deliberately not a project management system**. Status lives
in git: a task is "done" when the commit lands on `main` and the
acceptance criteria are checked off in the commit message or PR
description.

If you want a quick visual of progress, run:

```sh
grep -l '^Status: done' docs/tasks/*.md   # if tasks self-report
# or
git log --since='5 days ago' --grep='task' --oneline
```

Don't build a Jira clone in markdown. The grilling docs + entrega + tasks
+ git history are enough state.

## When *not* to use a task file

- **Trivial fixes** (typo, rename, formatting) — just do them.
- **Decisions that haven't been made yet** — go back to grilling first.
- **Cross-cutting refactors** — promote to a `/gsd-plan-phase` mini-phase.
