# Task 01 - Max - AI client and JSON models

**Owner:** Max
**Estimated effort:** ~2 hours
**Prerequisites:** `00-max-provider-and-prompt-contract`, `00-melanie-bootstrap-android`
**Day:** 1
**Blocks:** real provider integration

## Goal

Implement the real Gemini-backed repository, including request construction,
HTTP call, JSON parsing, and fallback on malformed responses.

## Inputs

- `docs/implementation_plan.md` sections 4-8
- `docs/adr/0001-gemini-first-ai-provider.md`
- `docs/adr/0002-safety-boundary-and-negative-prompts.md`
- Provider contract from task 00

## Outputs

Create or update:

- `data/ai/gemini/GeminiRestClient.kt`
- `data/repository/GeminiAiChatRepository.kt`
- `data/ai/GeminiConfig.kt` or equivalent
- Unit tests for JSON parser and request builder

## Acceptance Criteria

- [ ] Real repository compiles.
- [ ] API key comes from `BuildConfig` or injected config, not a committed
  string.
- [ ] Request includes system instruction, student input, and negative cases.
- [ ] Successful provider JSON maps to `AiFeedback`.
- [ ] Malformed provider response returns a local fallback or `Result.failure`
  handled by the ViewModel.
- [ ] `./gradlew :app:testDebugUnitTest` passes for parser/request tests.

## Pitfalls / Notes

- Gemini REST response nesting is provider-specific; isolate it inside
  `data/ai/gemini/`.
- Never render provider raw text directly in the UI.
- Keep timeout errors distinguishable from parse errors so the UI can show a
  useful retry message.
