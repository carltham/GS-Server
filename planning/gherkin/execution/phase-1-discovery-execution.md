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

## UI Module Structure
- Angular source: GSServer-pom/GSServer-UI/frontend
- Browser delivery: GSServer-pom/GSServer-UI
- Static delivery path: Spring Boot static resources in GSServer-pom/GSServer-UI

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

## Environment Mishap Log

### Mishap 1: Node/npm missing at first bootstrap
- Symptom: `npm: command not found`
- Impact: Playwright dependencies could not be installed.
- Resolution applied: Installed Node.js and npm via apt.

### Mishap 2: Browser host dependencies missing
- Symptom: Playwright reported missing Linux libraries (notably for WebKit).
- Impact: WebKit project failed to launch.
- Resolution to apply:
	1. `cd tests/ui`
	2. `npx playwright install-deps`

### Mishap 3: Angular app not running during smoke test
- Symptom: `ERR_CONNECTION_REFUSED` / `NS_ERROR_CONNECTION_REFUSED` at configured `BASE_URL`.
- Impact: Chromium/Firefox smoke test failed on `page.goto('/')`.
- Resolution to apply:
	1. Start Spring Boot UI app (serving Angular static build) on configured `BASE_URL`.
	2. Re-run `npm test` from `tests/ui`.

### Mishap 4: UI module structure was missing (resolved)
- Symptom: Planned module path `GSServer-pom/GSServer-UI/frontend` was not present in repository.
- Impact: No runnable Spring Boot UI target existed for Playwright smoke and phase tests.
- Resolution applied:
	1. Created module skeleton at `GSServer-pom/GSServer-UI`.
	2. Created Angular workspace placeholder at `GSServer-pom/GSServer-UI/frontend`.
	3. Added static host placeholder at `GSServer-pom/GSServer-UI/src/main/resources/static/index.html`.
- Remaining work:
	1. Implement Angular app in `frontend`.
	2. Configure Angular build output to Spring Boot static resources.
	3. Start Spring Boot app on `BASE_URL` and re-run Playwright.

## Re-run Checklist
1. `cd tests/ui`
2. `npm install`
3. `npx playwright install`
4. `npx playwright install-deps`
5. Start Spring Boot UI app on `BASE_URL` (default: `http://localhost:8080`).
6. `npm test`

## UI Module Bootstrap Checklist (Current)
1. Spring Boot module scaffold exists at `GSServer-pom/GSServer-UI`.
2. Angular workspace placeholder exists at `GSServer-pom/GSServer-UI/frontend`.
3. Configure Angular build output to `GSServer-pom/GSServer-UI/src/main/resources/static`.
4. Expose Spring Boot app on `http://localhost:8080`.
5. Verify `/` serves Angular index.
6. Re-run `tests/ui` Playwright suite.
