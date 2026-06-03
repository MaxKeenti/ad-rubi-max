# ADR-0002 - Safety boundary and negative prompts

**Status:** Accepted
**Date:** 2026-06-02

## Context

The classroom notes require that each AI request be conditioned with negative
cases and say the output must not have anything to do with killing.

The app domain also invites students to describe fears and worries. That means
some users may mention self-harm, suicide, violence, or severe distress even
though the assignment asks only for motivational feedback.

## Decision

Every AI request must include a safety-oriented system instruction and explicit
negative cases.

The app classifies obvious unsafe or crisis text before calling the provider.
For unsafe/crisis content, it returns a local support response instead of a
normal motivational answer.

The app must not:

- provide instructions for killing, murder, self-harm, suicide, or violence,
- encourage harm,
- include graphic harm details,
- diagnose mental illness,
- provide therapy or treatment plans,
- claim to replace a counselor, teacher, doctor, psychologist, or emergency
  service.

The app may:

- validate the user's feelings,
- encourage contacting a trusted person,
- provide Mexico support resources,
- provide a short grounding/study-oriented next step for non-crisis academic
  stress.

## Consequences

**Positive**

- Directly satisfies the classroom negative-prompt requirement.
- Makes the demo testable with clear negative cases.
- Reduces the chance that model output drifts into unsafe content.
- Gives a defensible boundary during review.

**Negative**

- Client-side keyword classification can over-block harmless text.
- Prompting is not a full safety system.
- The app must handle emotionally heavy inputs without pretending to be a
  professional service.

## Required Support Resources

For Mexico-focused demos, show:

- `Linea de la Vida: 800 911 2000`
- `Emergencias: 911`

For U.S.-configured demos, optionally show:

- `988 Suicide & Crisis Lifeline: call, text, or chat 988`

## Trigger To Revisit

Revisit if:

- the professor gives a precise safety rubric,
- the school asks for counselor escalation,
- the app is used outside a controlled classroom demo,
- a real mental-health professional reviews and changes the wording.
