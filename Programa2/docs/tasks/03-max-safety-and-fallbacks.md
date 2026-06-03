# Task 03 - Max - Safety classifier and fallbacks

**Owner:** Max
**Estimated effort:** ~1.5 hours
**Prerequisites:** task 00 contract, task 01 client optional
**Day:** 1-2
**Blocks:** CP2 negative-case acceptance

## Goal

Add local guardrails for obvious unsafe/crisis input and deterministic fallback
responses for provider failures.

## Inputs

- `docs/CONTEXT.md` sections "Negative Prompting And Safety Boundary" and
  "Crisis Response"
- `docs/implementation_plan.md` sections 6, 8, 9
- `docs/adr/0002-safety-boundary-and-negative-prompts.md`

## Outputs

Create or update:

- `data/ai/SafetyClassifier.kt`
- `data/ai/LocalFallbackResponses.kt`
- Unit tests for classifier categories

Classifier categories should map to:

- normal motivational input,
- crisis or self-harm,
- violence/killing request,
- out-of-scope.

## Acceptance Criteria

- [ ] CP2-07 returns no harm instructions.
- [ ] CP2-08 returns Linea de la Vida and 911 resources.
- [ ] Ordinary academic fear is not over-blocked.
- [ ] Provider failure still produces a useful local response.
- [ ] Unit tests cover representative positive and negative cases.

## Pitfalls / Notes

- Keyword matching is intentionally conservative but should not classify every
  mention of "miedo" or "estres" as crisis.
- Do not write graphic examples into test names or UI strings.
- The classifier is a course guardrail, not a real clinical triage system.
