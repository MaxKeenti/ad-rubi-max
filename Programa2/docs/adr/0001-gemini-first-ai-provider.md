# ADR-0001 - Gemini-first AI provider

**Status:** Accepted
**Date:** 2026-06-02

## Context

The classroom notes say the app must subscribe to one AI provider and list:

- Google Gemini API,
- OpenRouter,
- Groq,
- Hugging Face.

The same notes draw the architecture as:

```text
App Android -> HTTP -> API IA -> Json -> UI Compose
```

Program 2 needs one provider decision before implementation because the HTTP
shape, authentication header, response format, and error handling differ by
provider.

## Decision

Use **Google Gemini API** as the primary v1 provider.

Hide it behind an `AiChatRepository` interface so the UI does not depend on
Gemini-specific classes or endpoint details.

Use structured JSON output for the motivational response whenever the provider
supports it. If the provider returns malformed JSON, use a local fallback.

## Consequences

**Positive**

- Matches the classroom-note preference that marks Gemini as the newer option.
- Works with direct HTTP from Android for a course prototype.
- Supports system instructions and JSON-shaped output.
- Keeps implementation smaller than a multi-provider router.

**Negative**

- A direct API key in a mobile app is not secret in a real deployment.
- The app becomes dependent on one provider's availability and quota.
- If the professor expects a different provider, the team must implement an
  adapter swap.

## Mitigations

- Store the key in `local.properties`; never commit it.
- Keep provider code isolated in `data/ai/gemini/`.
- Keep `AiChatRepository` provider-neutral.
- Keep OpenRouter/Groq/Hugging Face as documented fallback options, not UI
  features.

## Alternatives Considered

- **OpenRouter first**: rejected for v1 because it adds routing/model choice
  complexity the assignment does not require.
- **Groq first**: plausible and fast, but the notes call Gemini the newer
  option.
- **Hugging Face first**: flexible, but provider/model selection adds setup
  overhead.
- **Backend proxy first**: better key security, but adds server work and moves
  away from the classroom Android -> HTTP -> AI API diagram.

## Trigger To Revisit

Revisit if:

- Gemini quota blocks the demo,
- the professor requires a different provider,
- the app moves from course prototype to real deployment,
- a backend proxy becomes mandatory for key protection.
