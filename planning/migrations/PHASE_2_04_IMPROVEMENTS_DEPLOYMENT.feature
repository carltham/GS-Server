# Migration: Phase 2 - Improvements & Deployment (2-3 days)
# Part 4 of Phase 2 Gateway Proxy Wiring Migration

Feature: Complete Phase 2 with additional improvements and production deployment
  As an infrastructure team
  I want to finalize Phase 2 with API enhancements and safely deploy to production
  So that we have a solid foundation for Phase 3+

  Background:
    Given JAR backend wiring is complete
    Given correlation IDs are fully implemented
    Given cross-tenant tests are 100% passing
    And security team is ready for final audit

  Scenario: Enhance error responses with full structured format
    Given Phase 1 has basic structured errors
    When we enhance error model:
      - Add documentationUrl field (if missing)
      - Add severity field (ERROR, WARN, INFO)
      - Add source field (ClassName.methodName)
      - Add requestId field (from correlationId)
      - Generate comprehensive error catalog documentation
      - Create error documentation page at /docs/api/errors
      - Link error codes to remediation steps
    Then error responses are fully structured per spec
    And documentation is auto-generated

  Scenario: Configure HTTPS/TLS for production
    Given proxy configuration is wired and working
    When we configure TLS:
      - Generate self-signed cert for development
      - Create TLS configuration for production
      - Add HSTS header: Strict-Transport-Security: max-age=31536000
      - Update proxy configuration to require TLS
      - Add TLS documentation and certificate management procedure
      - Test TLS connection handshake
    Then production can enforce HTTPS
    And certificate management is documented

  Scenario: Generate OpenAPI specification
    Given all REST endpoints are defined in jar
    When we add Springdoc-OpenAPI:
      ```xml
      <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.0.0</version>
      </dependency>
      ```
      - Add @OpenAPIDefinition to jar application
      - Add @Tag annotations to controllers
      - Add @Schema annotations to DTOs
      - Generate OpenAPI spec at /v3/api-docs
      - Swagger UI available at /swagger-ui.html
      - Document all endpoints with examples
    Then API specification is auto-generated and current
    And API documentation is browsable

  Scenario: Create Phase 2 deployment runbook
    Given all Phase 2 components are ready
    When we create deployment runbook:
      - Pre-deployment checklist
      - Step-by-step deployment procedure
      - Rollback procedure with exact commands
      - Post-deployment verification steps
      - Monitoring setup for critical metrics
      - Troubleshooting guide for common issues
    Then deployment is documented and repeatable

  Scenario: Integration test Phase 2 end-to-end
    Given all Phase 2 components are implemented
    When we run end-to-end flow:
      - UI at localhost:4200
      - Jar at localhost:8081
      - POST /api/v1/hardening with tenant A
      - Response includes correlationId
      - Audit log includes correlationId
      - Cross-tenant access denied
      - Error response is structured with errorId
      - No secrets in error message
    Then Phase 2 components work together correctly
    And all integrations are verified

  Scenario: Security audit Phase 2
    Given Phase 2 implementation is complete
    When we verify all security requirements:
      - [ ] UI calls jar via REST HTTP (no direct injection)
      - [ ] All operations traced with correlationId
      - [ ] Cross-tenant denial tests 100% passing
      - [ ] Error responses structured with errorId
      - [ ] Correlation IDs flow through all layers
      - [ ] No regression in Phase 1 features
      - [ ] TLS enforced for proxy configs
      - [ ] OpenAPI spec generated
      - [ ] Error documentation complete
      - [ ] Team trained on new architecture
    Then Phase 2 passes security review
    And security team approves deployment

  Scenario: Production deployment Phase 2
    Given Phase 2 is tested and security-approved
    When we deploy:
      - Create backup of current jar version
      - Deploy new jar module (port 8081)
      - Update UI configuration to point to jar
      - Verify both services running
      - Run smoke tests through UI
      - Monitor error rates and latency
      - Verify correlation IDs in logs
      - Verify audit logs writing
    Then:
      - ✅ UI-to-jar communication working
      - ✅ Correlation IDs enabled for tracing
      - ✅ Cross-tenant isolation verified
      - ✅ Structured errors in production
      - ✅ No regressions from Phase 1
      - ✅ Can scale services independently

  Scenario: Post-deployment Phase 2 verification
    Given Phase 2 is deployed to production
    When we verify after 24-48 hours:
      - Check error logs for any issues
      - Verify correlation IDs in audit logs
      - Test cross-tenant queries work correctly
      - Run a few hardening operations
      - Verify all logging working
      - Monitor CPU/memory/storage (no impact)
      - Verify team can use new error codes
    Then production deployment is stable
    And all Phase 2 features are working

  Scenario: Phase 2 completion verification
    Given Phase 2 deployment is stable in production
    When we verify all acceptance criteria:
      - [ ] UI calls jar backend via REST HTTP
      - [ ] All operations traced with correlation IDs
      - [ ] Cross-tenant denial tests 100% passing
      - [ ] Error responses structured with errorId, code, details
      - [ ] TLS enforced for proxy configurations
      - [ ] OpenAPI spec generated and current
      - [ ] Team trained and confident
      - [ ] Runbook documented
    Then Phase 2 is marked COMPLETE ✅
    And Phase 3 (Firewall Control) can begin
    And retrospective documents learnings for future phases

