# Task 02 - Melanie - Chat UI

**Owner:** Melanie
**Estimated effort:** ~2 hours
**Prerequisites:** `00-melanie-bootstrap-android`, repository/model contract from task 00
**Day:** 1
**Blocks:** final ViewModel integration

## Goal

Build the Compose chat surface against a fake/static state so the UI is ready
before the real AI client lands.

## Inputs

- `docs/CONTEXT.md`
- `docs/implementation_plan.md` sections 1, 4, 5, 9
- `AiFeedback` and `ChatMessage` models from task 00

## Outputs

Create or update:

- `ui/chat/ChatScreen.kt`
- `ui/chat/ChatUiState.kt`
- `ui/chat/ChatMessageBubble.kt` or local composables
- `MainActivity.kt` to show the chat screen

UI should include:

- message list,
- input text field,
- send button,
- loading indicator,
- clear chat action,
- error/retry surface,
- support-resource card for crisis/unsafe category.

## Acceptance Criteria

- [ ] Screen renders at phone size without overlapping text.
- [ ] Send button is disabled for blank input.
- [ ] Loading state prevents repeated sends.
- [ ] Assistant response can render message, next steps, follow-up question,
  and resources.
- [ ] UI works with fake data before real provider is available.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Pitfalls / Notes

- This is an app screen, not a landing page.
- Keep the visual style calm and utilitarian; the target user is stressed.
- Do not include visible instructions about prompt engineering or API internals.
- Make the input/action row stable so loading state does not resize it.
