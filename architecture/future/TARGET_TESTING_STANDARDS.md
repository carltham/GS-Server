# GS-Server Target Testing Standards

**Project:** GS-Server  
**Version:** 1.0 (Target State)  
**Last Updated:** 2026-07-24

---

## Testing Philosophy

**Goal:** Every change increases confidence, not just code coverage.

**Principle:** Tests define behavior; code implements it (RED-GREEN-REFACTOR).

---

## Test Tiers & Coverage Targets

### By Tier (Present → Target)

| Tier | Current | Target | Scope |
|---|---|---|---|
| **Unit** | 50%+ tests | 80%+ coverage | Individual methods |
| **Integration** | Partial | 70%+ coverage | Service + Adapter |
| **E2E** | Playwright | 90% happy path | Full user flow |
| **Security** | ❌ MISSING | **Mandatory** | Cross-tenant, secrets |

### By Layer (Present → Target)

| Layer | Current | Target | Examples |
|---|---|---|---|
| **Handler** | 90% | 100% | Request/response, auth |
| **Service** | 85% | 100% | Business logic |
| **Adapter** | 75% | 100% | Platform execution |
| **Repository** | 50% | 100% | Persistence |

---

## Test Requirements (MANDATORY)

### 1. Deterministic Tests

**Same input → same output, always.**

**MUST NOT depend on:**
- ❌ Current time (inject Clock)
- ❌ Random numbers (inject Random)
- ❌ Network (mock HTTP/RPC)
- ❌ External files (use temp directories)
- ❌ Environment variables (inject parameters)
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

### 2. Independence

**Tests don't depend on execution order.**

**MUST NOT:**
- ❌ Depend on prior test state
- ❌ Share mutable objects
- ❌ Use global state
- ❌ Create ordering requirements

---

### 3. Cross-Tenant Denial Tests (MANDATORY, CRITICAL)

**Every data-access feature tested for isolation.**

**Current state:** ❌ NO CROSS-TENANT TESTS
**Target state:** ✅ Comprehensive denial tests by Phase 2

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

### 4. Secrets Protection Tests

**Secrets must NOT appear in test output.**

**Pattern - REQUIRED:**
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

### 5. Regression Testing

**Bug fix = test first, then code.**

**Process:**
1. Write test that reproduces bug (FAILS with current code)
2. Fix the bug (test PASSES)
3. Commit both together

---

## Test Organization

### File Structure

```
src/test/java/com/gsserver/ui/
├── hardening/
│   ├── DefaultHardeningServiceTest
│   ├── HardeningControllerContractTest
│   └── adapter/
│       ├── LinuxHardeningAdapterTest
│       ├── WindowsHardeningAdapterTest
│       └── ProcessHardeningCommandExecutorTest
├── gateway/
│   ├── DefaultGatewayProxyServiceTest
│   └── GatewayProxyControllerContractTest
├── proxy/
│   ├── DefaultProxyServiceTest
│   └── ProxyControllerContractTest
├── auth/
│   └── AuthControllerContractTest
├── admin/
│   └── UserManagementServiceTest
├── db/
│   └── FileBasedHardeningOperationStateRepositoryTest
└── integration/
    ├── CrossTenantDenialTests
    ├── AuthorizationTests
    └── E2EWorkflowTests
```

### Naming Convention

**GOOD - describes what should happen:**
```
testUserCannotAccessOtherTenantsData()
shouldRejectUnauthorizedUser()
shouldApplyHardeningProfile()
```

**BAD - describes implementation:**
```
testMethod1()
testGetUser()
testSomething()
```

---

## Test Types

### Contract Tests (API Boundaries)

**All public endpoints tested for request/response contract.**

**Test EVERY status code:**
- ✅ 200 OK, 201 Created, 202 Accepted
- ✅ 400 Bad Request, 401 Unauthorized, 403 Forbidden
- ✅ 404 Not Found, 422 Unprocessable, 500 Server Error

### Integration Tests (Service + Adapter)

**Service + Adapter together, Repository mocked.**

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

### E2E Tests (Full Flow)

**Real services, mocked only external APIs.**

```java
@Test
public void hardeningShouldCompleteEndToEnd() {
  // Use real services
  HardeningService service = ...
  GatewayProxyService proxyService = ...
  
  // Mock only external (ProcessBuilder mocked)
  MockOsAdapter mockOs = ...
  
  // Execute full flow
  HardeningResult result = service.harden(...);
  
  // Verify: state persisted, response correct, logs captured
  assertThat(result.status()).isEqualTo("success");
  assertThat(fileSystem.read("operation-state.json"))
    .contains(result.operationId());
}
```

---

## Coverage Targets

| Component | Target | Threshold |
|---|---|---|
| Controllers | 100% | Enforce in CI |
| Services | 100% | Enforce in CI |
| Adapters | 100% | Enforce in CI |
| Repositories | 85%+ | Enforce in CI |
| **Overall** | 85%+ | Fail build if below |

**Measurement:**
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Test Data Safety

**NEVER use in tests:**
- ❌ Production credentials
- ❌ Real personal data (names, emails, phones)
- ❌ Live accounts or services
- ❌ Real database backups
- ❌ Production API keys

**DO use:**
- ✅ Test users: `test-operator@company.com`
- ✅ Test tenants: `TENANT_A`, `TENANT_B`
- ✅ Sandbox APIs
- ✅ Temporary files/directories
- ✅ Mock objects

---

## Pre-Release Testing Checklist

- [ ] All unit tests passing (100%)
- [ ] All integration tests passing (100%)
- [ ] All E2E tests passing (100%)
- [ ] Code coverage ≥ 85%
- [ ] Cross-tenant denial tests written & passing
- [ ] Regression tests for all recent bugs
- [ ] No secrets in test data/logs
- [ ] Error scenarios tested
- [ ] Edge cases tested (empty, large payloads)
- [ ] Rollback tested for all operations
- [ ] Contract tests validate error responses

---

## Summary

**Target testing requirements:**
- ✅ Deterministic + independent tests
- ✅ Cross-tenant denial tests (MANDATORY)
- ✅ Secrets protection tests
- ✅ Regression testing (test first)
- ✅ Contract tests for all endpoints
- ✅ Integration tests for services
- ✅ E2E tests for user flows
- ✅ Coverage > 85% enforced in CI

**Critical blocking gap:** NO cross-tenant denial tests (Phase 2 task)

