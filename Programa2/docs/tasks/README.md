# Tasks - agent-executable work units

Each `.md` file in this directory is one Program 2 task. The naming convention
matches Program 1:

```text
NN-<owner>-<short-name>.md
```

- `NN` is the sequence prefix.
- `<owner>` is `max` or `melanie`.
- `<short-name>` is a kebab-case summary.

## How To Use A Task File

From a fresh Codex/Claude session:

```text
@docs/tasks/02-melanie-chat-ui.md

Please execute this task. Stop and ask if anything is ambiguous.
```

The agent should read the task file, follow the input links, create or modify
only the listed outputs, and verify the acceptance criteria.

## Task File Structure

Every task file has:

- owner,
- estimated effort,
- prerequisites,
- day,
- goal,
- inputs,
- outputs,
- acceptance criteria,
- pitfalls/notes.

## Order Of Execution

Day-0 tasks must happen first:

```text
00-melanie-bootstrap-android
00-max-provider-and-prompt-contract
        |
        v
01-max-ai-client-and-json-models
02-melanie-chat-ui
        |
        v
03-max-safety-and-fallbacks
04-melanie-viewmodel-wireup
        |
        v
05-max-tests-and-demo-script
06-melanie-final-docs-and-screenshots
```

Max and Melanie can work in parallel after the day-0 contract review.

## Status Tracking

Status lives in git. A task is done when its acceptance criteria are satisfied
and the commit lands on `main`.

Do not turn this directory into a project management system. It is a set of
execution specs.
