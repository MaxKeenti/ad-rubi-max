# Tasks — agent-executable work units

Same convention as Programa1 (`Programa1/docs/tasks/README.md`):

```
NN-<owner>-<short-name>.md
```

- `NN` — sequence prefix, roughly chronological. Within a day, respect
  the **Prerequisites** declared inside each file.
- `<owner>` — `max` (all development + automated tests) or `melanie`
  (manual testing + delivery evidence). Unlike P1 this is a **role**
  split, not parallel dev lanes — see `../work-division.md`.
- `<short-name>` — kebab-case summary.

## How to use a task file from a Claude session

```
/task-run 06
```

The agent reads the task file, follows the **Inputs** links, produces
the **Outputs**, and verifies the **Acceptance criteria**. For Melanie,
`/explicar-tarea NN` produces the Spanish narrative explanation.

## Day mapping (schedule in `../implementation_plan.md` §5)

| Day | Tasks | Checkpoint |
|---|---|---|
| Jun 11 | 00–04 | App runs against fakes |
| Jun 12 | 04–07 | **TRIGGER:** camera → Firestore → real map marker, or cut confirmar + delete-own |
| Jun 13 | 08–11 | Feature-complete |
| Jun 14 | 12–15 | Distributed build in Melanie's hands |
| Jun 14–15 | 16–18 | Ship |

## Contingency (plan §5, decided in advance)

If the Jun 12 trigger fails: **09** shrinks to a read-only detail sheet
(cut confirmar + eliminar; schema keeps `confirmCount`), and the next
rung is cutting **10** (Recientes). Tasks 00–08, 11–12 are untouchable —
they are the rubric (geo, maps, storage, camera, rules).

## Status tracking

Status lives in git plus an optional `**Status:** done (date)` line in
the task header. No Jira clones in markdown.
