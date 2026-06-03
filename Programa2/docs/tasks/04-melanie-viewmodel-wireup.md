# Task 04 - Melanie - ViewModel wire-up

**Owner:** Melanie
**Estimated effort:** ~1.5 hours
**Prerequisites:** `02-melanie-chat-ui`, repository contract; real repository optional
**Day:** 2
**Blocks:** end-to-end demo

## Goal

Connect `ChatScreen` to `ChatViewModel` and `AiChatRepository`, first with the
fake repository and then with the real Gemini repository when available.

## Inputs

- `docs/implementation_plan.md` sections 4, 5, 9
- `docs/work-division.md` section 4.2
- `AiChatRepository` from task 00
- `ChatScreen` from task 02

## Outputs

Create or update:

- `ui/chat/ChatViewModel.kt`
- dependency wiring for fake/real repository
- `MainActivity.kt` or navigation entry point

## Acceptance Criteria

- [ ] Sending a message appends a student bubble immediately.
- [ ] Loading state appears while repository call runs.
- [ ] Assistant bubble appears from repository response.
- [ ] Errors show a retry/fallback state without crashing.
- [ ] Clear chat resets the conversation.
- [ ] Rapid repeated taps do not create duplicate in-flight requests.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Pitfalls / Notes

- Keep ViewModel state immutable.
- Do not let the UI call the Gemini client directly.
- If the real provider is blocked, ship the fake binding with a visible
  documented limitation for the checkpoint.
