# AnimoChat - Domain Context

Canonical domain language for Programa2. Implementation details belong in
`implementation_plan.md`; this file defines what the app is and is not.

The current source of truth is `source-notes-2026-06-02.md`, transcribed from
the two attached classroom-note images.

---

## Working Name

**AnimoChat** is the working name for the case-practical app. The final app
name can change without changing the domain model.

## Case Practical 2

The classroom case asks for a chat application where a student can explain
their fears, worries, and academic concerns, then receive motivational
feedback.

The problem frame is educational, not clinical: high rates of failing grades
and student discouragement can affect academic performance. The app supports
the student with encouragement, study-oriented reflection, and small next
steps.

## Student

A student is the person using the chat. The app does not need a school account
or identity system for v1. The student can use the app anonymously during the
demo.

## Motivational Feedback

A motivational response is a short AI-generated answer that:

- acknowledges the student's concern,
- avoids judgement,
- reframes the situation in a realistic way,
- suggests one to three small academic actions,
- asks at most one follow-up question when useful.

It is not therapy, diagnosis, emergency counseling, grading advice, or a
replacement for school staff, family, teachers, counselors, psychologists, or
medical professionals.

## Chat Message

A chat message is one piece of conversation shown in the Compose UI. Messages
have a role:

- `student` for user input,
- `assistant` for motivational feedback,
- `system` only inside the AI request and never shown as a normal chat bubble.

The UI can keep the conversation in memory for v1. Persistence is optional and
should not block the core delivery.

## AI Response

The AI provider returns JSON, not free-form UI text. The app parses that JSON
into an `AiFeedback` model and renders it in Compose.

Canonical response fields:

- `category`: `motivational`, `academic_stress`, `crisis_or_unsafe`, or
  `out_of_scope`.
- `message`: the main supportive response.
- `nextSteps`: zero to three concrete student actions.
- `followUpQuestion`: optional one-question continuation prompt.
- `resources`: optional crisis/support resources for unsafe or crisis cases.

If parsing fails, the app shows a local fallback response instead of exposing
raw JSON or crashing.

## Negative Prompting And Safety Boundary

The classroom notes explicitly require each request to be conditioned with
negative cases and say the response must not have anything to do with killing.

For this app, "negative prompting" means every AI request includes a safety
instruction that rejects or safely redirects content involving:

- killing, murder, or harming another person,
- self-harm or suicide,
- instructions for violence,
- graphic violent detail,
- medical or psychological diagnosis,
- definitive treatment plans.

The assistant may still respond supportively when the student mentions fear,
stress, sadness, failure, or anxiety. It must not provide methods, plans,
instructions, or encouragement for harm.

## Crisis Response

If a student's message clearly suggests immediate danger, self-harm, suicide,
or harm to someone else, the app must not continue as an ordinary motivational
chat. It should:

- acknowledge the situation briefly and calmly,
- say the app cannot handle emergencies,
- encourage contacting a trusted person nearby,
- show Mexico's `Linea de la Vida: 800 911 2000`,
- show emergency services `911`,
- optionally show U.S. `988` only when the app is configured for U.S. demo
  users.

The response must avoid giving harm-related details.

## AI Provider

The classroom notes list these possible providers:

- Google Gemini API,
- OpenRouter,
- Groq,
- Hugging Face.

For v1, the plan uses Google Gemini API as the primary provider because the
notes mark it as the newer option and current official docs support text
generation, system instructions, REST calls, and structured JSON output.

The implementation should still hide the provider behind an `AiChatRepository`
interface so OpenRouter, Groq, or Hugging Face can replace Gemini later without
rewriting the UI.

## Architecture

The classroom-note architecture is:

```text
Android app -> HTTP -> AI API -> JSON -> Compose UI
```

Program 2 should preserve that shape:

```text
Compose screen
    |
ViewModel
    |
AiChatRepository
    |
GeminiRestClient
    |
HTTP request to AI API
    |
JSON response parsed into AiFeedback
    |
Compose renders bubbles, next steps, and resource cards
```

## API Key

The API key is a development secret. It must not be committed.

For the course prototype, the key can be read from `local.properties` and
injected into `BuildConfig`. If a real deployment is pursued, the key should
move behind a backend or serverless proxy because mobile app binaries cannot
keep API keys secret.

## Course Topics

The notes also mention:

- multiplatform,
- PWA,
- Backend as a Service,
- offline-first.

These are treated as related course vocabulary. They are not required v1 scope
unless the professor explicitly asks for them in the Program 2 rubric.
