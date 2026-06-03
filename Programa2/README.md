# AnimoChat - Student Motivational AI Chat

Planning workspace for Programa2 in 6NM61 Programacion Movil. The app concept
comes from the attached classroom notes: an Android chat where a student writes
academic fears or worries and receives motivational feedback from an AI API.

## Current Status

Program 2 is in the planning phase. There is no Android scaffold yet. The
canonical source and execution docs are under `docs/`.

## Planned Scope

- Android app built with Kotlin and Jetpack Compose.
- Chat UI for student concerns and AI motivational feedback.
- HTTP call to one AI provider, planned as Google Gemini API for v1.
- JSON response parsing before rendering in Compose.
- Negative prompt/safety policy for killing, self-harm, suicide, and violence
  cases.
- Manual CP2 test runbook before delivery.

## Documentation

| Need | File |
|---|---|
| Classroom-note source | `docs/source-notes-2026-06-02.md` |
| Domain context | `docs/CONTEXT.md` |
| Implementation plan | `docs/implementation_plan.md` |
| Work split | `docs/work-division.md` |
| Architecture decisions | `docs/adr/` |
| Agent task specs | `docs/tasks/` |

## Planned Build Commands

After the Android scaffold exists:

```sh
cd Programa2
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Team

- Gonzalez Calzada Maximiliano
- Sosa Montoya Melanie Rubi

Course: 6NM61 Programacion Movil, UPIICSA.
