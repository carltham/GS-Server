# Migration: Phase 2 - Jar Backend Wiring (1 week)
# Part 1 of Phase 2 Gateway Proxy Wiring Migration

Feature: Wire jar backend as REST service for multi-service architecture
  As a platform architect
  I want the jar module to expose services via REST API
  So that services can scale independently and UI is stateless

  Background:
    Given Phase 1 (Foundation) is complete and verified
    And TARGET_DEVELOPMENT_RULES.md defines 6-tier architecture
    And team understands REST API principles

  Scenario: Analyze current direct injection architecture
    Given we have present-state code where UI calls services directly
    When we map current architecture:
      ```
      Angular UI (port 4200)
          ↓ direct method call
      GSServer-UI module (port 8080)
          ├─ HardeningController
          ├─ GatewayProxyController
          └─ Services (injected @Autowired)
      ```
    Then we identify:
      - Controllers call services directly (no HTTP)
      - Services have Spring dependencies
      - No REST API contract defined
      - UI cannot scale independently

  Scenario: Move service implementations to jar module
    Given services are currently in GSServer-UI
    When we migrate to jar module:
      - Create target package structure in GSServer-jar:
        ```
        com.gsserver.jar/
        ├── hardening/
        │   ├── DefaultHardeningService
        │   ├── HardeningService (interface)
        │   └── adapters/
        ├── gateway/
        │   ├── DefaultGatewayProxyService
        │   ├── GatewayProxyService (interface)
        │   └── adapters/
        └── proxy/
            ├── DefaultProxyService
            ├── ProxyService (interface)
            └── ProxyInstallationService
        ```
      - Move service implementations to jar
      - Move adapters to jar
      - Move repositories to jar
      - Keep DTOs in shared package
    Then service code is in jar module ready for REST exposure

  Scenario: Create REST controllers in jar module
    Given services are moved to jar
    When we create REST controllers:
      ```java
      // In GSServer-jar module
      @RestController
      @RequestMapping("/api/v1/hardening")
      public class HardeningHandler {
        @Autowired
        private HardeningService hardeningService;

        @PostMapping
        @PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
        public ResponseEntity<HardeningResponse> harden(
            @RequestBody HardeningRequest request,
            @RequestHeader("X-Correlation-ID") String correlationId) {
          return ResponseEntity.accepted()
            .header("X-Correlation-ID", correlationId)
            .body(hardeningService.triggerHardening(request));
        }
      }
      ```
      - Create HardeningHandler in jar
      - Create GatewayProxyHandler in jar
      - Create ProxyHandler in jar
      - All handlers delegate to services
      - All handlers accept/return correlationId
    Then jar module exposes complete REST API

  Scenario: Wire UI to call jar backend via REST HTTP
    Given jar module has REST controllers
    When we update GSServer-UI to:
      - Remove service implementations
      - Add HTTP client (RestTemplate or WebClient)
      - Create service proxies that call jar:
        ```java
        // In GSServer-UI module
        @Service
        public class RestHardeningService implements HardeningService {
          @Autowired
          private RestTemplate restTemplate;

          @Override
          public HardeningResponse triggerHardening(HardeningRequest request) {
            String correlationId = MDC.get("correlationId");
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Correlation-ID", correlationId);

            return restTemplate.exchange(
              "http://localhost:8081/api/v1/hardening",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              HardeningResponse.class).getBody();
          }
        }
        ```
      - Create RestGatewayProxyService wrapper
      - Create RestProxyService wrapper
      - Handle HTTP errors gracefully
    Then UI calls jar via REST HTTP

  Scenario: Test jar-to-UI communication
    Given UI is wired to call jar via HTTP
    When we run integration tests:
      | test | action | jar_response | ui_result |
      | hardening_request | POST /api/v1/hardening | 202 Accepted | HardeningResponse received |
      | error_handling | POST invalid input | 422 Unprocessable | ValidationException caught |
      | timeout_handling | jar slow | 504 Gateway Timeout | TimeoutException caught |
      | correlation_id | POST with header | response echoes header | correlationId flows |
    Then UI successfully communicates with jar backend
    And error responses are properly handled

  Scenario: Deploy jar module to production
    Given UI-to-jar communication is tested
    When we deploy:
      - Build GSServer-jar as standalone JAR
      - Start jar on port 8081 (separate from UI port 8080)
      - Configure UI to point to http://localhost:8081
      - Verify both services running
      - Run end-to-end test through UI to jar
    Then:
      - ✅ UI at port 8080 is stateless
      - ✅ Jar at port 8081 handles business logic
      - ✅ Both can be scaled independently
      - ✅ Requests flow: UI → Jar → Adapters
      - ✅ No regressions in functionality

