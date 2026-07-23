phase-1-discovery-execution.md

# Phase 1 Top-Down Angular Test Execution

Source scenario: planning/gherkin/phases/phase-1-foundation-and-defence.feature
Primary rule: Top-down TDD, one boundary at a time

## Objective
Validate Phase 1 using a strict top-down Angular test flow where UI is thin/dumb and all authoritative logic is enforced in backend boundaries.

## Playwright Infrastructure
- Workspace: tests/ui
- Config: tests/ui/playwright.config.ts
- Phase 1 test seed: tests/ui/tests/phase1/foundation-hardening.spec.ts
- Smoke test: tests/ui/tests/smoke/app-shell.spec.ts
- Setup instructions: tests/ui/README.md

## Boundary Order (Mandatory)
1. Angular UI test (user flow and API call expectation)
2. API contract test (request/response schema and structured errors)
3. Controller orchestration test (delegation only)
4. Service rule test (business and policy logic)
5. Adapter/command execution test (Linux and Windows hardening behavior)

Do not start a lower boundary until the boundary above has a failing test first.

## Test Execution Matrix

| Boundary | Scenario Focus | Test Type | Status | Evidence | Notes |
|---|---|---|---|---|---|
| Angular UI | Operator triggers hardening action from UI | Component/integration (Angular) |  |  |  |
| REST API | Hardening endpoint contract and error model | API contract/integration |  |  |  |
| Controller | Request routed with orchestration only | Unit |  |  |  |
| Service | Validation, authz, policy, audit trigger | Unit/integration |  |  |  |
| Linux Adapter | Hardening command execution and exit handling | Integration |  |  |  |
| Windows Adapter | Hardening command execution and exit handling | Integration |  |  |  |

## Mandatory Assertions
- Angular contains no domain or policy decisions.
- API errors follow structured error model.
- Controller contains orchestration only.
- Service contains authoritative business and policy logic.
- Adapter captures stdout, stderr, and exit code.
- Correlation and audit context are recorded for hardening actions.

## Execution Steps
1. Write failing Angular test for hardening trigger and expected API call.
2. Write failing API contract test and structured error test.
3. Write failing controller orchestration test.
4. Write failing service policy test.
5. Write failing Linux adapter execution test.
6. Write failing Windows adapter execution test.
7. Implement only enough code to pass each boundary in sequence.
8. Refactor with all tests green.

## Completion Criteria
- All boundary tests pass in top-down order.
- Cross-cutting quality gates pass.
- Phase 1 can proceed to next scenario only after boundary evidence is recorded.
