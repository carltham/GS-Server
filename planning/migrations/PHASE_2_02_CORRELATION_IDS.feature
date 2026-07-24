# Migration: Phase 2 - Correlation IDs (2 days)
# Part 2 of Phase 2 Gateway Proxy Wiring Migration

Feature: Implement correlation IDs to trace requests across services
  As a platform operator
  I want requests to be traceable across UI and jar services
  So that I can debug issues and trace operations end-to-end

  Background:
    Given Phase 2 jar wiring is complete
    And Phase 1 audit logging includes correlationId field
    And team understands MDC (Mapped Diagnostic Context)

  Scenario: Design correlation ID flow across services
    Given UI calls jar via REST HTTP
    When we define correlation ID strategy:
      - UI generates UUID for correlationId (if not provided)
      - UI includes correlationId in all jar requests: `X-Correlation-ID: <uuid>`
      - Jar echoes correlationId in all responses
      - Jar includes correlationId in all audit logs
      - Jar includes correlationId in all error responses
      - Client-side stores correlationId for debugging
      - Logs automatically include correlationId via MDC
    Then correlation IDs enable end-to-end request tracing

  Scenario: Implement CorrelationIdFilter in jar
    Given correlation ID strategy is defined
    When we add CorrelationIdFilter to jar:
      ```java
      @Component
      public class CorrelationIdFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

          String correlationId = request.getHeader("X-Correlation-ID");
          if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
          }

          MDC.put("correlationId", correlationId);
          response.setHeader("X-Correlation-ID", correlationId);

          try {
            filterChain.doFilter(request, response);
          } finally {
            MDC.remove("correlationId");
          }
        }
      }
      ```
      - Add filter to SecurityFilterChain
      - Extract correlationId from request header
      - Generate new UUID if not provided
      - Store in MDC for automatic logging
      - Echo in response headers
    Then every request has unique, traceable correlationId

  Scenario: Thread correlationId through service layer
    Given CorrelationIdFilter is implemented
    When we update services to use MDC:
      ```java
      @Service
      public class DefaultHardeningService {
        public HardeningResult harden(HardeningRequest request) {
          String correlationId = MDC.get("correlationId");

          HardeningResult result = adapter.execute(request);

          // correlationId automatically included in audit log via MDC
          auditLogger.log(...);  // includes correlationId from MDC

          return result;
        }
      }
      ```
      - Remove explicit correlationId parameter passing
      - Use MDC.get("correlationId") where needed
      - Update logging configuration to include MDC
      - Audit logs automatically include correlationId
    Then correlationId flows through all layers via MDC

  Scenario: Update error responses to include correlationId
    Given correlationId is available via MDC
    When we update error handler:
      ```java
      @RestControllerAdvice
      public class ApiExceptionHandler {
        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleException(Exception e) {
          String correlationId = MDC.get("correlationId");
          return ResponseEntity.status(500).body(new ErrorResponse(
            ...
            correlationId: correlationId,
            ...
          ));
        }
      }
      ```
      - Add correlationId to all error responses
      - Include correlationId in response headers
      - Ensure correlationId is never null
    Then error responses include correlationId for tracing

  Scenario: Add correlationId to UI service calls
    Given jar includes correlationId in responses
    When we update UI RestTemplate configuration:
      ```java
      @Configuration
      public class RestTemplateConfig {
        @Bean
        public RestTemplate restTemplate() {
          RestTemplate template = new RestTemplate();
          template.getInterceptors().add((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            request.getHeaders().set("X-Correlation-ID", correlationId);
            return execution.execute(request, body);
          });
          return template;
        }
      }
      ```
      - Add interceptor to include correlationId in all requests
      - Extract correlationId from response headers
      - Store in MDC for logging
      - Display in UI for debugging
    Then UI automatically propagates correlationId

  Scenario: Test correlation ID flow end-to-end
    Given correlation IDs are wired through all layers
    When we run tracing test:
      - POST /api/v1/hardening with X-Correlation-ID: test-123
      - Trace request through UI → jar → service → adapter → audit log
      - Verify all logs include correlationId: test-123
      - Verify response echoes X-Correlation-ID: test-123
      - Verify error responses include correlationId
    Then requests are traceable across services
    And support can locate full operation history by correlationId

