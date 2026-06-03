# Task 05 - Max - Tests and demo script

**Owner:** Max
**Estimated effort:** ~1 hour
**Prerequisites:** tasks 01, 03, 04
**Day:** 2
**Blocks:** final confidence before checkpoint

## Goal

Create the test/runbook material needed to prove the Program 2 requirements,
especially the negative prompt cases.

## Inputs

- `docs/implementation_plan.md` section 9
- `docs/adr/0002-safety-boundary-and-negative-prompts.md`
- Existing unit tests from tasks 01 and 03

## Outputs

Create:

- `docs/guia-pruebas-programa2.md`

Include:

- CP2-01 through CP2-10,
- exact test input,
- expected behavior,
- pass/fail column,
- notes column.

Update tests if a CP2 case exposes a parser/classifier gap.

## Acceptance Criteria

- [ ] Manual runbook exists.
- [ ] CP2 negative cases are explicit.
- [ ] Unit tests pass.
- [ ] Build command passes or failures are documented with exact cause.
- [ ] No secrets appear in docs, logs, or screenshots.

## Pitfalls / Notes

- Do not paste API keys into screenshots or docs.
- Keep negative-case examples short and non-graphic.
- Manual tests should be executable by Melanie without reading source code.
