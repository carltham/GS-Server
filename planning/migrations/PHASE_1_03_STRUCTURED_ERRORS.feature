# Migration: Phase 1 - Structured Error Model (3 days)
# Part 3 of Phase 1 Foundation & Defence Migration

Feature: Implement structured error responses with error codes for programmatic handling
  As an API consumer
  I want standardized error responses with stable error codes
  So that I can handle errors programmatically and debug issues

  Background:
    Given Phase 1 secrets redaction and audit logging are complete
    And TARGET_API_STANDARDS.md defines the error response schema
    And all developers understand the error model

  Scenario: Define ErrorResponse schema and ErrorCatalog
    Given we need structured, consistent error responses
    When we create ErrorResponse class:
      | field | type | required | example |
      | errorId | string | yes | HARDENING_FAILED_001 |
      | code | string | yes | HARDENING_FAILED |
      | message | string | yes | Hardening operation failed |
      | timestamp | ISO8601 | yes | 2026-07-24T10:00:00Z |
      | correlationId | UUID | yes | req-12345 |
      | severity | enum | yes | ERROR, WARN, INFO |
      | source | string | yes | DefaultHardeningService.harden() |
      | details | JSON | yes | {exitCode: 1, timedOut: false} |
      | retryable | boolean | yes | false |
      | documentationUrl | URL | yes | https://docs/errors/HARDENING_FAILED |
    And we create ErrorCatalog with all error types:
      | errorId | code | message | retryable | when |
      | HARDENING_FAILED_001 | HARDENING_FAILED | Hardening operation failed | false | adapter.execute() throws |
      | PROXY_CONFIG_FAILED_001 | PROXY_CONFIG_FAILED | Proxy configuration failed | false | nginx -t fails |
      | AUTH_FAILED_001 | AUTH_FAILED | Authentication failed | false | invalid credentials |
      | VALIDATION_FAILED_001 | VALIDATION_FAILED | Request validation failed | false | invalid input |
      | TIMEOUT_001 | TIMEOUT | Operation timed out | true | execution > timeout |
      | SERVER_ERROR_999 | SERVER_ERROR | Unexpected server error | false | unhandled exception |
    Then error catalog is documented and accessible

  Scenario: Replace generic error handler with structured errors
    Given ErrorResponse schema is defined and ErrorCatalog created
    When we update ApiExceptionHandler:
      ```java
      @RestControllerAdvice
      public class ApiExceptionHandler {
        @ExceptionHandler(HardeningExecutionException.class)
        public ResponseEntity<?> handleHardeningError(HardeningExecutionException e) {
          return ResponseEntity.status(500).body(new ErrorResponse(
            errorId: "HARDENING_FAILED_001",
            code: "HARDENING_FAILED",
            message: "Hardening operation failed (check documentation)",
            timestamp: Instant.now().toString(),
            correlationId: MDC.get("correlationId"),
            severity: "ERROR",
            source: e.getStackTrace()[0].toString(),
            details: {exitCode: e.getExitCode(), timedOut: e.isTimedOut()},
            retryable: false,
            documentationUrl: "https://docs/errors/HARDENING_FAILED"
          ));
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<?> handleValidationError(ValidationException e) {
          return ResponseEntity.status(422).body(new ErrorResponse(
            errorId: "VALIDATION_FAILED_001",
            code: "VALIDATION_FAILED",
            message: "Request validation failed",
            // ... other fields
          ));
        }
      }
      ```
    Then all exception types map to structured ErrorResponse

  Scenario: Generate error documentation
    Given ErrorCatalog is complete with all error types
    When we generate error documentation:
      - Create error documentation page at /docs/api/errors
      - List all errorIds with descriptions
      - Show example error responses
      - Provide remediation steps
      - Link to relevant API documentation
    And we update API documentation with error references
    Then error documentation is auto-generated and up-to-date

  Scenario: Test structured error responses
    Given structured error handler is implemented
    When we write tests for error scenarios:
      | scenario | action | expected_errorId | expected_retryable |
      | hardening_timeout | hardening > 5min | HARDENING_FAILED_001 | false |
      | proxy_invalid_config | nginx -t fails | PROXY_CONFIG_FAILED_001 | false |
      | invalid_request | POST with bad JSON | VALIDATION_FAILED_001 | false |
      | timeout_retry | operation times out | TIMEOUT_001 | true |
      | unexpected_error | unhandled exception | SERVER_ERROR_999 | false |
    And we verify all fields are present:
      ```java
      @Test
      public void errorResponseHasAllRequiredFields() {
        ResponseEntity<?> response = executeFailingOperation();
        ErrorResponse error = (ErrorResponse) response.getBody();

        assertThat(error.errorId()).isNotEmpty();
        assertThat(error.code()).isNotEmpty();
        assertThat(error.message()).isNotEmpty();
        assertThat(error.timestamp()).isNotEmpty();
        assertThat(error.correlationId()).isNotEmpty();
        assertThat(error.severity()).isNotEmpty();
        assertThat(error.source()).isNotEmpty();
        assertThat(error.details()).isNotNull();
        assertThat(error.retryable()).isNotNull();
        assertThat(error.documentationUrl()).isNotEmpty();
      }
      ```
    Then all error responses are properly structured
    And no error response contains original secrets

  Scenario: Client integration testing
    Given structured errors are in production
    When we verify clients can:
      - Parse errorId and code programmatically
      - Make retry decisions based on retryable field
      - Display user-friendly message
      - Log error for debugging with correlationId
      - Access documentation via documentationUrl
    Then error structure is client-friendly and usable

  Scenario: Production deployment of structured errors
    Given structured error model is complete and tested
    When we deploy to production:
      - Deploy new version with structured errors
      - Verify error responses are structured and safe
      - Monitor error rates (no regressions)
      - Update client code to handle new format
      - Announce error documentation to API users
    Then:
      - ✅ All errors are structured with errorId
      - ✅ Error documentation is accessible
      - ✅ Clients can handle errors programmatically

