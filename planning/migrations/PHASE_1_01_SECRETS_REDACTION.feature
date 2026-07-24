# Migration: Phase 1 - Secrets Redaction (3 days)
# Part 1 of Phase 1 Foundation & Defence Migration

Feature: Implement secrets redaction to prevent credential exposure in error messages
  As a security officer
  I want to ensure no credentials leak in error messages or API responses
  So that GS-Server can be safely exposed to external logging services

  Background:
    Given the team has reviewed TARGET_SECURITY_POLICY.md
    And the secrets redaction rule is understood
    And CI/CD pipeline is operational with test suite passing

  Scenario: Identify all error message leakage points
    Given we have present-state code with potential secret exposure
    When we run automated secrets detection scan
      | scan_type | tool | config |
      | grep | git-secrets | secrets.baseline |
      | grep | truffleHog | default |
      | scan | sonarqube | OWASP_injection |
    Then we identify all files containing:
      | pattern | example |
      | database_connection | "Failed to connect: user=admin password=..." |
      | ssh_key_path | "SSH key not found at /home/thor/.ssh..." |
      | process_output | raw stderr with credentials |
      | stack_traces | file paths and system details |
    And we create issue tickets for each leakage point
    And we add secrets to .gitignore patterns

  Scenario: Create ErrorRedactor utility class
    Given we have identified all error message leakage points
    When we create ErrorRedactor utility with redaction methods:
      | method | input | output |
      | redactPasswordsFromUrl | "user:pass@host" | "user:***@host" |
      | redactFilePaths | "/root/.ssh/id_rsa" | "/root/.ssh/***" |
      | redactProcessOutput | stderr with secrets | sanitized output |
      | redactErrorContext | full error object | safe error object |
    And we add comprehensive unit tests for each method:
      ```java
      @Test
      public void redactPasswordFromUrl() {
        String input = "jdbc:mysql://admin:secret@db";
        String result = redactor.redactPasswordsFromUrl(input);
        assertThat(result).isEqualTo("jdbc:mysql://admin:***@db");
        assertThat(result).doesNotContain("secret");
      }
      ```
    Then ErrorRedactor is implemented with 100% test coverage

  Scenario: Wire secrets redaction into ApiExceptionHandler
    Given ErrorRedactor is implemented and tested
    When we update ApiExceptionHandler:
      ```java
      @RestControllerAdvice
      public class ApiExceptionHandler {
        @Autowired
        private ErrorRedactor redactor;

        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleException(Exception e) {
          String original = e.getMessage();
          String redacted = redactor.redact(original);
          logger.error("Full error: {}", original);  // Internal logs retain full context
          return ResponseEntity.status(500).body(
            new ApiErrorResponse(redacted));  // API response is safe
        }
      }
      ```
    Then all error responses pass automated secrets scan
    And error messages are human-readable without exposing credentials

  Scenario: Test secrets redaction across all error paths
    Given ErrorRedactor is wired into exception handler
    When we write tests for each error scenario:
      | scenario | input_error | expected_in_response | should_not_contain |
      | database_failure | "user=admin password=secret123" | "database connection failed" | secret123 |
      | ssh_missing | "/root/.ssh/id_rsa" | "SSH key not found" | /root/.ssh |
      | process_failed | stderr with API key | "process execution failed" | api_key |
      | file_error | full path with credentials | "file error" | credentials |
    Then all error paths pass redaction tests
    And no API response contains original secrets
    And internal logs (secured endpoints) retain full context

  Scenario: Team training and pre-commit hooks
    Given secrets redaction is implemented and tested
    When we:
      - Train team on redaction rules
      - Add pre-commit hook to catch secret patterns
      - Update developer wiki with examples
      - Document what NOT to log in API responses
    Then team is trained on secrets protection
    And pre-commit hook prevents accidental secret commits

  Scenario: Secrets redaction verification
    Given all secrets redaction implementation is complete
    When we run final verification:
      - Deploy to staging environment
      - Trigger various error scenarios
      - Scan API responses for credentials (0 findings)
      - Verify internal logs contain full context
      - Check pre-commit hook catches test secrets
    Then secrets redaction is production-ready

