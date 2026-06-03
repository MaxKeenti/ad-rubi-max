# Task 00 - Melanie - Bootstrap Android project

**Owner:** Melanie
**Estimated effort:** ~2 hours
**Prerequisites:** none
**Day:** 0
**Blocks:** all implementation work

## Goal

Create a buildable Android Compose project under `Programa2/` with a
placeholder screen. No real AI logic yet.

## Inputs

- `docs/implementation_plan.md` sections 1-4
- Current `Programa1/` Gradle conventions, only as a reference

## Outputs

Create the standard Android project files under `Programa2/`.

Recommended package root:

```text
com.example.animochat
```

Create package directories:

```text
data/ai/
data/ai/gemini/
data/model/
data/repository/
ui/chat/
ui/theme/
```

The placeholder screen can show:

```text
AnimoChat - bootstrap OK
```

## Acceptance Criteria

- [ ] `cd Programa2 && ./gradlew :app:assembleDebug` succeeds.
- [ ] App launches on emulator with a Compose placeholder.
- [ ] No Gemini/API dependency is required for the placeholder to run.
- [ ] No API key is committed.
- [ ] Max can pull the branch and add model/repository files.

## Pitfalls / Notes

- Keep the initial scaffold small.
- Do not copy Program 1 Firebase setup unless this Program 2 rubric actually
  needs Firebase.
- If adding HTTP dependencies now, prefer versions that compile cleanly with
  the Gradle/Kotlin setup.
