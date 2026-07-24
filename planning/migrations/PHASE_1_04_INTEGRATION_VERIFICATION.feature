# Migration: Phase 1 - Integration & Verification
# Part 4 of Phase 1 Foundation & Defence Migration

Feature: Verify all Phase 1 components work together and meet production readiness
  As an infrastructure team
  I want to verify Phase 1 is complete and production-ready
  So that we can confidently deploy to production

  Background:
    Given all Phase 1 components are implemented:
      - ✅ Secrets redaction
      - ✅ Audit logging
      - ✅ Structured errors
    And all unit tests are passing
    And security team is ready to audit

  Scenario: End-to-end hardening operation test
    Given all Phase 1 components are wired together
    When we execute hardening operation:
      ```
      POST /api/v1/hardening
      {
        "tenantId": "tenant-a",
        "profile": "strict",
        "requestedBy": "operator@company.com"
      }
      ```
    Then:
      - ✅ Response is 202 Accepted
      - ✅ Response includes correlationId in header
      - ✅ Response body includes correlationId
      - ✅ If error: structured ErrorResponse with errorId
      - ✅ Error message contains NO secrets
      - ✅ Audit log entry created with actor=operator@company.com
      - ✅ Audit log includes action=hardening.apply
      - ✅ Audit log includes full operation details
      - ✅ Audit log is queryable by tenant/actor/date

  Scenario: Cross-component integration verification
    Given all Phase 1 components are implemented
    When we run integration tests:
      | component_flow | verification |
      | Secrets → Error Handler | Error message redacted, logs contain full context |
      | Audit Logger → MDC | correlationId flows through service layers |
      | Structured Error → Audit | Error response is parseable, audit log accurate |
      | All together → Production | End-to-end flow works without issues |
    Then all components integrate correctly

  Scenario: Security audit Phase 1
    Given Phase 1 implementation is complete
    When we run security checklist:
      - [ ] No secrets in error messages (automated scan: 0 findings)
      - [ ] All error responses have errorId + code
      - [ ] Audit logging covers all write operations
      - [ ] Correlation IDs flow through requests
      - [ ] Error documentation is complete
      - [ ] Audit logs can be exported for compliance
      - [ ] Retention policy is enforced
      - [ ] Team is trained on new components
    Then security team approves Phase 1 for production

  Scenario: Production deployment Phase 1
    Given Phase 1 is security-approved and tested
    When we deploy to production:
      - Create backup of existing logs
      - Deploy new version with all Phase 1 components
      - Verify error responses are structured and safe
      - Verify audit logging working and writing to secure location
      - Monitor error rates (no regressions)
      - Verify secrets redaction (no credentials exposed)
      - Run smoke tests through key workflows
    Then:
      - ✅ Secrets redaction active in production
      - ✅ Audit logging writing successfully
      - ✅ Structured errors being returned
      - ✅ No regressions in operation success rate
      - ✅ Security team satisfied with audit trail

  Scenario: Post-deployment verification
    Given Phase 1 is deployed to production
    When we verify after 24 hours:
      - Check error logs for any issues
      - Verify audit log volume and retention
      - Test audit export functionality
      - Run a few hardening operations and verify all logging
      - Monitor CPU/memory/storage (no impact)
      - Verify team can query audit logs
    Then production deployment is stable
    And all Phase 1 features are working as designed

  Scenario: Rollback procedure (if needed)
    Given Phase 1 is deployed but issues discovered
    When we need to rollback:
      - Stop current version
      - Start previous version
      - Verify error responses are back to generic format
      - Verify no further audit logs being written
      - Investigate root cause
      - Fix and redeploy when ready
    Then rollback is quick and safe

  Scenario: Phase 1 completion verification
    Given Phase 1 deployment is stable in production
    When we verify all acceptance criteria:
      - [ ] All hardening operations logged with actor/timestamp
      - [ ] Error messages don't expose credentials
      - [ ] Structured error model with errorId, code, details
      - [ ] Real server integration testing complete
      - [ ] Production deployment working stably
      - [ ] Team trained and confident
    Then Phase 1 is marked COMPLETE ✅
    And team begins Phase 2 planning
    And retrospective is scheduled to capture learnings

