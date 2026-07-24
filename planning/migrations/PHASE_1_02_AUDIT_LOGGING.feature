# Migration: Phase 1 - Audit Logging Framework (5 days)
# Part 2 of Phase 1 Foundation & Defence Migration

Feature: Implement audit logging framework for compliance and debugging
  As a compliance officer
  I want a complete audit trail of all write operations
  So that we can meet regulatory requirements and debug issues

  Background:
    Given Phase 1 secrets redaction is complete
    And the audit logging schema is defined in TARGET_SECURITY_POLICY.md
    And AuditLog domain model is understood by the team

  Scenario: Design audit logging schema and repository
    Given we need to track who did what when
    When we create AuditLog domain model with fields:
      | field | type | required | example |
      | timestamp | ISO8601 | yes | 2026-07-24T10:00:00Z |
      | correlationId | UUID | yes | req-12345-67890 |
      | actor | email | yes | operator@company.com |
      | tenantId | string | yes | tenant-a |
      | action | enum | yes | hardening.apply |
      | resource | string | yes | server-prod-01 |
      | result | enum | yes | success, failed |
      | details | JSON | yes | {profile: strict} |
      | errorReason | string | no | timeout |
    And we create AuditLogRepository with methods:
      | method | params | return |
      | save | AuditLog | void |
      | findByTenant | tenantId, dateRange | List<AuditLog> |
      | findByActor | actor, dateRange | List<AuditLog> |
      | export | tenantId, format | CSV/JSON |
    Then audit schema is implemented in FileBasedAuditRepository

  Scenario: Wire audit logging into HardeningService
    Given AuditLog and repository are implemented
    When we update DefaultHardeningService:
      ```java
      @Service
      public class DefaultHardeningService {
        @Autowired
        private AuditLogger auditLogger;

        public HardeningResult harden(HardeningRequest req) {
          try {
            HardeningResult result = adapter.execute(req);
            auditLogger.log(new AuditLog(
              timestamp: Instant.now(),
              correlationId: getCurrentCorrelationId(),
              actor: SecurityContextHolder.getContext().getName(),
              tenantId: req.tenantId(),
              action: "hardening.apply",
              resource: req.serverId(),
              result: result.success() ? "success" : "failed",
              details: {profile: req.profile(), platform: result.platform()}
            ));
            return result;
          } catch (Exception e) {
            auditLogger.log(AuditLog(...result: "failed", errorReason: e.getClass().getSimpleName()));
            throw e;
          }
        }
      }
      ```
    Then hardening operations are logged with complete context

  Scenario: Wire audit logging into other services
    Given audit logging is working in HardeningService
    When we add audit logging to:
      | service | events_to_log |
      | DefaultGatewayProxyService | proxy.configure, proxy.rollback |
      | UserManagementService | user.create, user.update, user.delete, user.resetPassword |
      | AuthController | auth.login, auth.logout |
      | TerminalSessionManager | terminal.session.start, terminal.session.end |
    Then all write operations are logged with actor and result
    And all audit logs include correlationId for request tracing

  Scenario: Implement audit log export API
    Given all services log audit events
    When we create GET /api/v1/audit/logs endpoint:
      ```java
      @RestController
      @RequestMapping("/api/v1/audit")
      @PreAuthorize("hasAuthority('GROUP_AUDIT_READERS')")
      public class AuditController {
        @GetMapping("/logs")
        public ResponseEntity<?> getLogs(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("format") String format) {
          return auditService.exportLogs(tenantId, startDate, endDate, format);
        }
      }
      ```
      - Format parameter: csv, json, pdf
      - Date range: ISO8601 dates
      - Tenant scoped: only own tenant visible
    Then audit logs are queryable and exportable

  Scenario: Verify audit logging completeness
    Given audit logging is wired into all services
    When we run verification tests:
      | test | action | assertions |
      | hardening_logged | POST /api/v1/hardening | audit entry: actor, action=hardening.apply, result |
      | user_create_logged | POST /api/v1/admin/users | audit entry: action=user.create |
      | proxy_config_logged | POST /api/v1/gateway/proxy/configure | audit entry: action=proxy.configure |
      | failed_auth_logged | invalid credentials | audit entry: result=failed |
      | export_works | GET /api/v1/audit/logs | CSV/JSON export successful |
    Then audit trail is complete for all write operations
    And audit logs pass compliance review

  Scenario: Implement audit log retention policy
    Given audit logs are being written to storage
    When we configure retention:
      - Retention period: 1+ year minimum
      - Archive after 30 days (move to cold storage)
      - Compress archives (reduce storage cost)
      - Backup encrypted and verified
      - No deletion (immutable append-only)
    Then audit logs are retained for compliance
    And storage costs are optimized with archival

  Scenario: Production deployment of audit logging
    Given audit logging is complete and tested
    When we deploy to production:
      - Enable audit logging to secure location (encrypted)
      - Verify audit logs writing successfully
      - Monitor audit log volume
      - Create runbook for compliance export
    Then:
      - ✅ Production operations start logging
      - ✅ Audit trail is complete and queryable
      - ✅ Compliance team can export as needed

