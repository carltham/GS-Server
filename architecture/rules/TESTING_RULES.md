# GS-Server Testing Rules

**Extracted from:** TARGET_TESTING_STANDARDS.md  
**Last Updated:** 2026-07-24

---

## Rule 1: Deterministic Tests (MANDATORY)

**Rule:** Same input → same output, always.

**MUST NOT depend on:**
- ❌ Current time (inject Clock)
- ❌ Random numbers (inject Random)
- ❌ Network (mock HTTP/RPC)
- ❌ External files (use temp directories)
- ❌ Environment variables (inject as parameters)
- ❌ Database state (clear before each test)

**Example - CORRECT:**
```java
@Test
public void hardeningShouldApplyProfile() {
  Clock clock = Clock.fixed(
    Instant.parse("2026-07-24T10:00:00Z"), UTC);
  
  // Inject real implementations, not mocks
  HardeningAdapter realAdapter = new LinuxHardeningAdapter();
  HardeningRepository realRepo = new FileBasedRepository();
  
  HardeningService service = new HardeningService(
    realAdapter, realRepo, clock);
  
  HardeningResult result = service.harden(request);
  
  assertThat(result.timestamp())
    .isEqualTo("2026-07-24T10:00:00Z");  // Deterministic
}
```

---

## Rule 2: Independent Tests (MANDATORY)

**Rule:** Tests don't depend on execution order.

**MUST NOT:**
- ❌ Depend on prior test state
- ❌ Share mutable objects between tests
- ❌ Use global state
- ❌ Create ordering requirements

**Pattern - CORRECT:**
```java
@Test
public void testA() {
  List<String> list = new ArrayList<>();  // ← Fresh instance
  list.add("a");
  assertThat(list).contains("a");
}

@Test
public void testB() {
  List<String> list = new ArrayList<>();  // ← Independent
  list.add("b");
  assertThat(list).contains("b");
}
```

---

## Rule 3: Cross-Tenant Denial Tests (MANDATORY)

**Rule:** Every data-access feature tested for isolation.

**Pattern - REQUIRED:**
```java
@Test
public void userFromTenantA_cannotAccess_tenantB_data() {
  User userA = createUserInTenant(TENANT_A);
  OperationState dataB = createOperationInTenant(TENANT_B);
  
  assertThatThrownBy(() ->
    service.getOperationState(dataB.id(), userA))
    .isInstanceOf(AccessDeniedException.class);
}

@Test
public void userFromTenantA_canAccess_tenantA_data() {
  User userA = createUserInTenant(TENANT_A);
  OperationState dataA = createOperationInTenant(TENANT_A);
  
  OperationState result = service.getOperationState(
    dataA.id(), userA);
  
  assertThat(result.id()).isEqualTo(dataA.id());
}
```

**Requirement:** For every service method that touches data, both tests required.

---

## Rule 4: Secrets Protection in Tests (MANDATORY)

**Rule:** Secrets must NOT appear in test output.

**Pattern - CORRECT:**
```java
@Test
public void errorMessages_do_not_expose_secrets() {
  String apiKey = "secret-key-12345";
  
  ApiResponse response = callWithSecret(apiKey);
  
  assertThat(response.getErrorMessage())
    .doesNotContain(apiKey)
    .doesNotContain("secret");
}

@Test
public void logs_do_not_expose_passwords() {
  String password = "super-secret-password";
  
  List<String> logs = captureLogsFor(() ->
    service.authenticate(username, password));
  
  assertThat(logs)
    .noneSatisfy(log ->
      assertThat(log).doesNotContain(password));
}
```

---

## Rule 5: Regression Testing (MANDATORY)

**Rule:** Bug fix = test first, then code.

**Process:**
1. Write test that reproduces bug (FAILS with current code)
2. Fix the bug (test PASSES)
3. Commit both together

**Pattern - REQUIRED:**
```java
// Test that fails with current code (bug reproduced)
@Test
public void hardeningRollback_shouldRevertToExactPriorState() {
  service.harden(request);
  HardeningResult rollbackResult = service.rollback();
  
  OperationState current = service.getLatestState();
  
  // This FAILS with current code, PASSES after fix
  assertThat(current.message())
    .isEqualTo("Rollback completed successfully");
}
```

---

## Rule 6: Test Naming Convention

**Rule:** Test names describe WHAT should happen, not implementation.

**✅ GOOD:**
```
testUserCannotAccessOtherTenantsData
shouldRejectUnauthorizedUser
shouldApplyHardeningProfile
hardeningShouldCompleteSuccessfully
```

**❌ BAD:**
```
testMethod1
testGetUser
testSomething
doThing
```

---

## Rule 7: Test Tier Coverage Targets (Iterative TDD Aligned)

**By Tier (Iterative TDD Strategy):**

| Tier | Target | Scope | When to Write |
|---|---|---|---|
| **Unit** | 20% (critical only) | Threading, DB transactions, file I/O errors | Only when unit test is faster than integration test |
| **Integration** | 90%+ coverage | Focused tests at layer boundaries (Iterative TDD) | EVERY feature: pause E2E → write focused IT for failing layer → implement → resume |
| **E2E** | 90% happy path | Full user flow (Playwright) | START every feature with E2E test |
| **Security** | 100% required | Cross-tenant denial, secrets redaction, authorization | Every data-access layer boundary |

**By Layer (Iterative TDD Focused Tests):**

| Layer | Strategy | What to Test |
|---|---|---|
| **Handler** | Integration test at Controller → Service boundary | Request validation, error redaction, response structure |
| **Service** | Integration test at Service → Adapter boundary | Business logic, audit logging, authorization |
| **Adapter** | Integration test at Adapter → External boundary | Platform execution, error handling, secrets redaction |
| **Repository** | Unit test ONLY if threading/transaction bugs found | Persistence, I/O, rollback behavior |

**Coverage Rationale:**
- ✅ **90% Integration:** Iterative TDD focused tests cover layer boundaries (fastest, most comprehensive)
- ✅ **20% Unit:** Only for subtle bugs (threading races, transaction isolation, file descriptor leaks)
- ✅ **90% E2E:** Playwright tests validate full feature works end-to-end

**Measurement:**
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
# Target: 90%+ integration coverage, 20% unit for critical paths only
```

---

## Rule 8: Test Organization (Iterative TDD Layer Boundaries)

**File structure (organized by layer boundary, not component):**
```
src/test/java/com/gsserver/
├── hardening/
│   ├── HardeningHandlerIntegrationTest   (Handler → Service boundary, Iterative TDD)
│   ├── HardeningServiceIntegrationTest   (Service → Adapter boundary, Iterative TDD)
│   ├── HardeningAdapterIntegrationTest   (Adapter → External boundary, Iterative TDD)
│   ├── LinuxHardeningAdapterUnitTest     (ONLY if threading/file I/O bugs found)
│   └── FileBasedStateRepositoryUnitTest  (ONLY if transaction bugs found)
├── gateway/
│   ├── GatewayProxyHandlerIntegrationTest   (Handler → Service boundary)
│   ├── GatewayProxyServiceIntegrationTest   (Service → Adapter boundary)
│   ├── NginxExecutorIntegrationTest         (Adapter → External boundary)
│   └── ProxyStateRepositoryUnitTest         (ONLY if persistence bugs found)
└── e2e/
    ├── HardeningE2ETest                     (Playwright, full flow)
    ├── ProxyE2ETest                         (Playwright, full flow)
    ├── CrossTenantDenialE2ETest             (Security, Playwright)
    └── SecretsRedactionE2ETest              (Security, Playwright)
```

**Naming convention:**
- `*IntegrationTest` = Iterative TDD focused test at layer boundary
- `*UnitTest` = Critical issue only (threading, DB, file I/O)
- `*E2ETest` = Playwright end-to-end validation

---

## Rule 9: Focused Integration Tests at Handler → Service Boundary

**Rule (Iterative TDD):** Handler layer integration tests validate request/response contract AND downstream layer failures.

**Test matrix (using Iterative TDD):**

| Scenario | What Fails | Iterative TDD Test |
|---|---|---|
| Valid request, happy path | (nothing) | Handler accepts → Service executes → returns 200 ✅ |
| Invalid input | Handler validation | Handler rejects → returns 422 ✅ |
| Unauthorized user | Authorization | Handler checks auth → returns 403 ✅ |
| Service throws exception | Service layer | **PAUSE E2E, write focused IT:** Handler catches exception, redacts secrets, returns 500 ✅ |
| Adapter throws exception | Adapter layer | **Continue E2E → fails at Service boundary, write focused IT:** Service catches, redacts, logs audit ✅ |
| External system fails (DB/Process) | External system | **Continue E2E → fails at Adapter boundary, write focused IT:** Adapter catches, redacts, returns error ✅ |

**Pattern - Stack-Based TDD at Handler → Service:**
```java
// E2E Playwright test
test('POST /api/v1/hardening with error → response redacted', async ({ page }) => {
  const response = await page.request.post('/api/v1/hardening', {
    data: { serverId: 'invalid' } // will trigger adapter error
  });
  expect(response.status()).toBe(500);
  const body = await response.json();
  expect(body.message).not.toContain('password'); // redacted
});

// Pause E2E when it fails at Handler layer
// Write focused integration test for Handler with REAL Service:
@Test
public void handler_callsRealService_andRedactsResponse() {
  // Use REAL Service implementation (not mock)
  HardeningService realService = new DefaultHardeningService(
    realAdapter, auditLog, clock);
  
  HardeningHandler handler = new HardeningHandler(
    realService, errorRedactor);
  
  ResponseEntity<?> response = handler.harden(validRequest());
  
  // Assert: handler accepted request, called real service, handled response
  assertThat(response.getStatusCode()).isEqualTo(200);
  assertThat(response.getBody()).hasFieldOrProperty("hardeningId");
}

// This test fails because Service throws unhandled exception
// → PAUSE this test, PUSH onto stack
// → Write focused test for Service layer with REAL Adapter
// → Repeat until all layers work

// Then POP tests back up: fix Adapter to match Service, fix Service to match Handler
```

**Key difference from traditional contract tests:**
- ❌ Old approach: Separate contract test, separate integration test
- ✅ Iterative TDD: One focused integration test per layer boundary, triggered by E2E failure

---

## Rule 10: Stack-Based Integration Tests (No Internal Mocks)

**Rule:** Service test calls REAL Adapter (not mocked). Only mock external service providers.

**Pattern - CORRECT:**
```java
@Test
public void service_callsRealAdapter_andHandlesResult() {
  // Real internal layers (NEVER mock these)
  HardeningAdapter realAdapter = new LinuxHardeningAdapter();
  HardeningRepository realRepo = new FileBasedRepository();
  
  // If Adapter calls external API (e.g., cloud provider), mock ONLY that:
  CloudProviderAPI mockCloudAPI = mock(CloudProviderAPI.class);
  when(mockCloudAPI.hardening(any())).thenReturn(success());
  realAdapter.setCloudProvider(mockCloudAPI);  // inject mocked external
  
  HardeningService service = new HardeningService(
    realAdapter, realRepo, clock);
  
  HardeningResult result = service.harden(request);
  
  // Assert: Service → Adapter integration works
  assertThat(result.operationId()).isNotNull();
  assertThat(realRepo.lastSavedState().operationId())
    .isEqualTo(result.operationId());
  // If fails: FIX ADAPTER to match Service's contract
}
```

**Key rule:** Mock only external service providers (third-party APIs). Never mock internal layers (Service, Adapter, Repository).

---

## Rule 11: E2E Tests (Full Flow, Stack-Based Authority)

**Rule:** E2E test runs against REAL full stack. E2E test is the ultimate authority—all layers must satisfy it.

**Pattern - CORRECT:**
```java
@Test
public void hardeningShouldCompleteEndToEnd() {
  // Real services (entire stack)
  HardeningService service = new DefaultHardeningService(...);
  DefaultGatewayProxyService proxyService = new DefaultGatewayProxyService(...);
  HardeningAdapter adapter = new LinuxHardeningAdapter();
  HardeningRepository repo = new FileBasedRepository();
  
  // Only use test doubles for TRUE external (subprocess, network)
  ProcessSandbox sandbox = new ProcessSandbox();  // Behaves like real OS, but isolated
  
  // Execute full flow
  HardeningResult result = service.harden(...);
  
  // Verify: persisted, response correct, logs captured
  assertThat(result.status()).isEqualTo("success");
  assertThat(repo.load(result.operationId()).status())
    .isEqualTo("success");
  // E2E test DOMINATES: if it fails, all layers adjust to make it pass
}
```

---

## Rule 12: Test Data Safety

**NEVER use in tests:**
- ❌ Production credentials
- ❌ Real personal data
- ❌ Live accounts or services
- ❌ Real database backups
- ❌ Production API keys

**DO use:**
- ✅ Test user: `test-operator@company.com`
- ✅ Test tenants: `TENANT_A`, `TENANT_B`
- ✅ Sandbox APIs
- ✅ Temporary files/directories
- ✅ Mock objects

---

## Rule 13: Pre-Release Testing Checklist

- [ ] All unit tests passing (100%)
- [ ] All integration tests passing (100%)
- [ ] All E2E tests passing (100%)
- [ ] Code coverage ≥ 85%
- [ ] Cross-tenant denial tests written & passing
- [ ] Regression tests for all recent bugs
- [ ] No secrets in test data/logs
- [ ] Error scenarios tested (invalid input, failure)
- [ ] Edge cases tested (empty lists, large payloads)
- [ ] Rollback tested for all operations
- [ ] Contract tests validate error responses

---

## Rule 14: Local Test Execution

**Run all tests:**
```bash
mvn clean test
```

**Run single test class:**
```bash
mvn clean test -Dtest=HardeningServiceTest
```

**Run with coverage:**
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

**Run specific test method:**
```bash
mvn clean test -Dtest=HardeningServiceTest#shouldApplyHardeningProfile
```

---

## Rule 15: CI/CD Pipeline Requirements

**On every commit:**
- ✅ Run all unit tests
- ✅ Run all integration tests
- ✅ Generate coverage report
- ✅ Fail build if coverage < 85%

**On PR:**
- ✅ Run tests
- ✅ Check coverage delta
- ✅ Enforce cross-tenant denial tests
- ✅ Verify no secrets in output

**Before release:**
- ✅ Full test suite
- ✅ E2E tests on staging
- ✅ Security scan
- ✅ Performance baseline

