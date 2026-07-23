definition-of-done-checklist.md

# Definition of Done Checklist

Use this checklist for every story, task, or capability increment.
A work item is not Done until every mandatory checkbox is satisfied.

## 1) Scope and Acceptance

- [ ] Story scope is explicit and time-bounded.
- [ ] Acceptance criteria are written and testable.
- [ ] Out-of-scope items are listed.

## 2) Thin Client Rule (Mandatory)

- [ ] UI changes are presentation-only and API-consumption-only.
- [ ] No domain/business/policy decision logic exists in Angular components/services.
- [ ] UI input validation is UX-focused only (authoritative validation remains backend).
- [ ] UI does not bypass backend contracts.

## 3) Iterative Top-Down TDD (Mandatory)

- [ ] First failing test was created at the highest boundary for this increment.
- [ ] Work progressed one boundary at a time.
- [ ] Lower-layer implementation started only after upstream failing test existed.
- [ ] RED -> GREEN -> REFACTOR cycle is visible in commit history or PR notes.

## 4) Boundary-by-Boundary Evidence

- [ ] API/contract tests exist and pass.
- [ ] Controller orchestration tests exist and pass.
- [ ] Service/business-rule tests exist and pass.
- [ ] Repository/adapter tests exist and pass.
- [ ] Boundary responsibilities remain separated (no leakage across layers).

## 5) Architecture Conformance

- [ ] Controllers orchestrate only.
- [ ] Executors implement only.
- [ ] Adapter behavior remains platform-specific and replaceable.
- [ ] No direct cross-layer shortcuts or forbidden dependencies were introduced.

## 6) Platform and Operations

- [ ] Linux behavior is verified for this increment.
- [ ] Windows behavior is verified where applicable.
- [ ] Command execution paths capture stdout, stderr, and exit code.
- [ ] Timeouts and failure handling are implemented for command execution.

## 7) Security and Auditability

- [ ] Authorization checks are enforced in backend boundaries.
- [ ] Sensitive operations are logged with correlation context.
- [ ] Validation and error handling follow the structured API model.
- [ ] No secrets are hardcoded or exposed in logs.

## 8) Quality Gates

- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Regressions are covered by new or updated tests.
- [ ] Code review confirms conformance to architecture and TDD rules.

## 9) Documentation and Traceability

- [ ] Relevant architecture/planning docs are updated.
- [ ] Gherkin scenario mapping is recorded.
- [ ] Operational notes/runbook updates are included where needed.
- [ ] Decision impacts and trade-offs are captured.

## 10) Release Readiness

- [ ] Feature is deployable behind current release controls.
- [ ] Rollback path is documented and validated.
- [ ] Monitoring/alerting implications are addressed.
- [ ] Final demo/verification against acceptance criteria is complete.

## Optional Per-Story Signoff

- Story/Task:
- Owner:
- Reviewer:
- Date:
- Result: [ ] Done  [ ] Not Done
- Notes:
