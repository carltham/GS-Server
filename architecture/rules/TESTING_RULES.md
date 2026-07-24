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
  
  HardeningService service = new HardeningService(
    mockAdapter, mockRepo, clock);
  
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

## Rule 7: Test Tier Coverage Targets

**By Tier:**

| Tier | Target | Scope |
|---|---|---|
| **Unit** | 80%+ coverage | Individual methods |
| **Integration** | 70%+ coverage | Service + Adapter |
| **E2E** | 90% happy path | Full user flow |
| **Security** | 100% required | Cross-tenant, secrets |

**By Layer:**

| Layer | Target | Examples |
|---|---|---|
| **Handler** | 100% | Request/response, auth |
| **Service** | 100% | Business logic |
| **Adapter** | 100% | Platform execution |
| **Repository** | 85%+ | Persistence, I/O |

**Measurement:**
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Rule 8: Test Organization

**File structure:**
```
src/test/java/com/gsserver/
├── hardening/
│   ├── HardeningHandlerContractTest      (API contract)
│   ├── DefaultHardeningServiceTest       (business logic)
│   ├── LinuxHardeningAdapterTest         (execution)
│   └── FileBasedHardeningStateRepositoryTest
├── gateway/
│   ├── GatewayProxyHandlerContractTest
│   ├── DefaultGatewayProxyServiceTest
│   ├── ProcessNginxCommandExecutorTest
│   └── FileBasedProxyStateRepositoryTest
└── integration/
    ├── CrossTenantDenialTests            (security)
    ├── AuthorizationTests                (security)
    └── E2EWorkflowTests                  (full flow)
```

---

## Rule 9: Contract Tests (API Boundaries)

**Rule:** All public endpoints tested for request/response contract.

**Test EVERY status code:**
- ✅ 200 OK → valid request, correct response
- ✅ 201 Created → new resource created
- ✅ 400 Bad Request → invalid input
- ✅ 401 Unauthorized → no authentication
- ✅ 403 Forbidden → no authorization
- ✅ 404 Not Found → resource missing
- ✅ 422 Unprocessable → validation failed
- ✅ 500 Server Error → unexpected error

**Pattern - REQUIRED:**
```java
@Test
public void validRequest_shouldReturn200() {
  HardeningRequest req = validRequest();
  ResponseEntity<?> response = handler.harden(req);
  
  assertThat(response.getStatusCode()).isEqualTo(OK);
  assertThat(response.getBody()).isNotNull();
}

@Test
public void invalidInput_shouldReturn422() {
  HardeningRequest req = new HardeningRequest(null); // invalid
  ResponseEntity<?> response = handler.harden(req);
  
  assertThat(response.getStatusCode())
    .isEqualTo(UNPROCESSABLE_ENTITY);
}

@Test
public void unauthorizedUser_shouldReturn403() {
  User unauthorized = createUserWithRole(VIEWER);
  HardeningRequest req = validRequest();
  
  assertThatThrownBy(() -> handler.harden(req))
    .isInstanceOf(AccessDeniedException.class);
}
```

---

## Rule 10: Integration Tests (Service + Adapter)

**Rule:** Service + Adapter together, Repository mocked.

**Pattern - RECOMMENDED:**
```java
@Test
public void shouldPersistHardeningStateAfterSuccess() {
  MockedRepository repo = new MockedRepository();
  HardeningService service = new HardeningService(
    realAdapter, repo, clock);
  
  HardeningResult result = service.harden(request);
  
  assertThat(repo.lastSavedState())
    .hasOperationId(result.operationId())
    .hasStatus("success");
}
```

---

## Rule 11: E2E Tests (Full Flow)

**Rule:** Real services, mocked only external APIs.

**Pattern - RECOMMENDED:**
```java
@Test
public void hardeningShouldCompleteEndToEnd() {
  // Real services
  HardeningService service = real();
  DefaultGatewayProxyService proxyService = real();
  
  // Mock only external
  MockOsAdapter mockOs = mock();
  
  // Execute full flow
  HardeningResult result = service.harden(...);
  
  // Verify: persisted, response correct, logs captured
  assertThat(result.status()).isEqualTo("success");
  assertThat(fileSystem.read("operation-state.json"))
    .contains(result.operationId());
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

