# Work Division - Max + Melanie

**Decided:** 2026-06-02
**Team:** Gonzalez Calzada Maximiliano, Sosa Montoya Melanie Rubi
**Planning checkpoint from notes:** Monday 2026-06-08
**Next delivery window from notes:** Monday 2026-06-15 to Friday 2026-06-19
**Branches:** `max`, `rubi` -> merge into `main`

This document captures the Program 2 work-division plan. Detailed task files
live in `docs/tasks/`. The "why" behind architectural decisions lives in
`docs/adr/`.

---

## 1. Coordination Model: Contract-First Parallel

The split is the same shape used in Program 1: define contracts first, then
let UI and data/provider work proceed in parallel.

The key contract is `AiChatRepository`. Once its request/response models are
accepted, Melanie can build the Compose chat UI against a fake repository while
Max implements the real Gemini HTTP client, prompt policy, safety classifier,
and parser tests.

## 2. Day-0 Bootstrap Sequence

```text
Max                               Melanie
--------------------------------  --------------------------------
Choose provider + key path
Draft prompt and JSON contract
Draft AiChatRepository
        |                         Bootstrap Compose project
        |                         Add package structure
        |                         Build placeholder screen
        v
30-min contract review sync
        |
Max implements real client         Melanie implements ChatScreen
        |
Integration: fake -> real repository binding
```

End of day 0 target: the Android scaffold compiles and both people agree on
the repository contract and JSON schema.

## 3. Component Ownership

| # | Item | Owner | Est. h |
|---|---|---:|---:|
| 0a | Android Compose scaffold in `Programa2/` | Melanie | 2 |
| 0b | Provider account/key setup and Gemini test curl | Max | 1 |
| 0c | Prompt policy, JSON schema, repository contract | Max, reviewed by Melanie | 1 |
| 1 | Data models and fake `AiChatRepository` | Max | 1 |
| 2 | Chat screen UI, message bubbles, input bar, loading state | Melanie | 2 |
| 3 | Gemini REST client and response parser | Max | 2 |
| 4 | ChatViewModel integration | Melanie, with Max support | 1.5 |
| 5 | Safety classifier and local fallback responses | Max | 1.5 |
| 6 | Manual test run CP2-01 through CP2-10 | Both | 1 |
| 7 | Screenshots and final docs refresh | Melanie + Max | 1.5 |

**Estimated total:** Melanie ~5.5 h direct implementation, Max ~6.5 h direct
implementation, both ~2.5 h shared verification/docs.

## 4. Integration Strategy

### 4.1 Branches

Keep the same person-named branch model as Program 1:

- Max works on `max`.
- Melanie works on `rubi`.
- Merge to `main` after each work session.

### 4.2 Fake -> Real Cutover

1. UI starts against `FakeAiChatRepository`.
2. Max lands `GeminiAiChatRepository`.
3. Hilt or manual dependency wiring swaps fake to real.
4. CP2 manual tests run with the real provider.
5. If provider access fails before the checkpoint, demo falls back to fake
   repository plus documented blocked provider setup.

### 4.3 Shared-File Ownership

| File | Primary owner | If the other needs to edit |
|---|---|---|
| `app/build.gradle.kts` | Melanie | Ping before dependency/plugin changes. |
| `MainActivity.kt` | Melanie | Coordinate nav/entry changes. |
| `ui/chat/*` | Melanie | Max can edit only for integration fixes. |
| `data/ai/*` | Max | Melanie can edit only after contract review. |
| `data/repository/AiChatRepository.kt` | Max, frozen after review | Reopen in a sync before changing. |
| `docs/` | Either | Keep source docs aligned after decisions. |

## 5. Agent Task Structure

Task files live in `docs/tasks/` and follow Program 1 naming:

```text
NN-<owner>-<short-name>.md
```

Each task file includes:

- owner,
- estimated effort,
- prerequisites,
- goal,
- inputs,
- outputs,
- acceptance criteria,
- pitfalls/notes.

## 6. Definition Of Done

Each implementation task is done when:

1. The app compiles with `./gradlew :app:assembleDebug`.
2. Relevant unit tests pass, when the task adds testable logic.
3. The related CP2 manual cases pass or the failure is documented.
4. No API keys or personal secrets are committed.
5. The docs still match the implementation.

## 7. Risks And Contingencies

| Risk | Likelihood | Mitigation |
|---|---|---|
| API key setup takes longer than expected | Medium | Keep fake repository demo-ready and document provider setup separately. |
| Direct mobile API key exposure | High for real deployments | Accept for course prototype only; use restricted key and no commit. Backend proxy deferred. |
| AI returns non-JSON text | Medium | Use structured output prompt/schema and parser fallback. |
| Safety prompt fails on negative cases | Medium | Add client-side classifier for obvious unsafe terms before provider call. |
| Scope creep into therapy/diagnosis | Medium | ADR-0002 locks the boundary to motivational support only. |
| PWA/multiplatform becomes required later | Unknown | Keep Android core simple; do not build PWA until rubric confirms it. |

## 8. Communication Cadence

- Day-0 sync: repository contract and JSON shape.
- Midpoint sync: fake UI is ready; real provider client is ready or blocked.
- Final sync before Monday 2026-06-08: run CP2 manual tests and capture demo
  screenshots if the UI exists.

## 9. Where To Find Things

| You want... | Look at... |
|---|---|
| What the classroom notes said | `docs/source-notes-2026-06-02.md` |
| What a domain term means | `docs/CONTEXT.md` |
| Why Gemini was chosen first | `docs/adr/0001-gemini-first-ai-provider.md` |
| Why negative prompts are mandatory | `docs/adr/0002-safety-boundary-and-negative-prompts.md` |
| The implementation plan | `docs/implementation_plan.md` |
| The executable task specs | `docs/tasks/` |
