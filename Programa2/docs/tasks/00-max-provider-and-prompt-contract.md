# Task 00 - Max - Provider and prompt contract

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** none
**Day:** 0
**Blocks:** real AI client, ViewModel integration, reliable negative-case demo

## Goal

Choose the provider configuration path and freeze the prompt/JSON/repository
contract that the UI will consume.

## Inputs

- `docs/source-notes-2026-06-02.md`
- `docs/CONTEXT.md`
- `docs/implementation_plan.md` sections 5-8
- `docs/adr/0001-gemini-first-ai-provider.md`
- `docs/adr/0002-safety-boundary-and-negative-prompts.md`

## Outputs

Create or update these files after the Android scaffold exists:

- `app/src/main/java/.../data/repository/AiChatRepository.kt`
- `app/src/main/java/.../data/model/ChatMessage.kt`
- `app/src/main/java/.../data/model/AiFeedback.kt`
- `app/src/main/java/.../data/model/SupportResource.kt`
- `app/src/main/java/.../data/ai/PromptPolicy.kt`

The repository contract should be provider-neutral:

```kotlin
interface AiChatRepository {
  suspend fun sendMessage(
    message: String,
    recentMessages: List<ChatMessage>,
  ): Result<AiFeedback>
}
```

## Acceptance Criteria

- [ ] Contract compiles.
- [ ] `AiFeedback` maps to the JSON shape in `implementation_plan.md`.
- [ ] Prompt policy includes explicit negative cases for killing, self-harm,
  suicide, and violence instructions.
- [ ] Melanie has reviewed the contract before UI work depends on it.
- [ ] No provider API key is committed.

## Pitfalls / Notes

- Keep Gemini details out of the repository interface.
- Do not expose raw JSON to the UI.
- Keep response fields small enough for reliable structured output.
- The prompt is not the only safety layer; task 03 adds local classification.
