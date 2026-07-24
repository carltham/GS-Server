# Migration: Phase 2 - Cross-Tenant Denial Tests (1 week)
# Part 3 of Phase 2 Gateway Proxy Wiring Migration

Feature: Implement cross-tenant denial tests to verify multi-tenant isolation
  As a security officer
  I want explicit tests verifying users cannot access other tenants' data
  So that we can prove multi-tenant isolation is working correctly

  Background:
    Given multi-tenancy is implemented in code but not tested
    Given JAR backend wiring is complete
    And team understands cross-tenant test patterns

  Scenario: Implement cross-tenant test fixtures
    Given we need to create users and data in different tenants
    When we create test setup:
      ```java
      public class CrossTenantTestFixtures {
        public static User createUserInTenant(String tenantId, String role) {
          ManagedUser user = new ManagedUser(
            username: "user-" + UUID.randomUUID(),
            password: passwordEncoder.encode("password"),
            authorities: List.of(role),
            tenantId: tenantId
          );
          return userStore.save(user);
        }

        public static HardeningOperationState createOperationInTenant(
            String tenantId, String status) {
          HardeningOperationState op = new HardeningOperationState(
            operationId: UUID.randomUUID().toString(),
            status: status,
            tenantId: tenantId
          );
          return repository.save(op);
        }
      }
      ```
      - Create test users for TENANT_A, TENANT_B, TENANT_C
      - Create test operations for multiple tenants
      - Fixtures support different roles (OPERATOR, ADMIN, SUPERUSER)
    Then test setup enables cross-tenant testing

  Scenario: Write hardening cross-tenant denial tests
    Given test fixtures create users/operations in different tenants
    When we write denial tests:
      ```java
      @Test
      public void userFromTenantA_cannotAccess_tenantB_operations() {
        User userA = createUserInTenant("tenant-a", "OPERATOR");
        HardeningOperationState opB = createOperationInTenant("tenant-b", "success");

        SecurityContext ctx = SecurityContextFactory.createSecurityContext(userA);
        SecurityContextHolder.setContext(ctx);

        assertThatThrownBy(() ->
          hardeningService.getLatestOperationState(opB.tenantId(), userA))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("tenant-b");
      }

      @Test
      public void userFromTenantA_canAccess_tenantA_operations() {
        User userA = createUserInTenant("tenant-a", "OPERATOR");
        HardeningOperationState opA = createOperationInTenant("tenant-a", "success");

        SecurityContext ctx = SecurityContextFactory.createSecurityContext(userA);
        SecurityContextHolder.setContext(ctx);

        HardeningOperationState retrieved =
          hardeningService.getLatestOperationState(opA.tenantId(), userA);

        assertThat(retrieved.operationId()).isEqualTo(opA.operationId());
      }
      ```
      - Test denial: user from tenant A cannot access tenant B
      - Test acceptance: user from tenant A can access tenant A
      - Test tenant validation in service layer
    Then cross-tenant hardening access is tested

  Scenario: Write proxy cross-tenant denial tests
    Given cross-tenant test pattern is established
    When we write denial tests for gateway proxy:
      | test_case | setup | action | expected_result |
      | user_A_denies_B_config | user=A, config=B | getLatestState(B) | AccessDeniedException |
      | user_A_allows_A_config | user=A, config=A | getLatestState(A) | operation returned |
      | user_A_denies_B_rollback | user=A, op=B | rollback(B) | AccessDeniedException |
      | admin_A_allows_A_only | admin=A | configure proxy B | AccessDeniedException |
    Then proxy operations are tested for cross-tenant denial

  Scenario: Write user management cross-tenant denial tests
    Given admin and user roles exist
    When we write authorization tests:
      | test_case | actor | action | resource | expected |
      | operator_cannot_create_user | operator | create | any | AccessDeniedException |
      | operator_cannot_modify_other | operator | update | tenant B user | AccessDeniedException |
      | admin_can_manage_own_tenant | admin-A | create | own tenant | success |
      | admin_cannot_manage_other_tenant | admin-A | create | tenant B user | AccessDeniedException |
      | superuser_can_manage_all | superuser | create | any tenant | success |
    Then user management is tested for cross-tenant denial

  Scenario: Run complete cross-tenant test suite
    Given all cross-tenant tests are implemented
    When we run full test suite:
      ```bash
      mvn test -Dtest=*CrossTenant*Test
      ```
    Then all tests pass:
      - ✅ Hardening: 4+ denial tests passing
      - ✅ Proxy: 4+ denial tests passing
      - ✅ Users: 5+ denial tests passing
      - ✅ No test data leaks between tenants
      - ✅ All layer isolation verified

  Scenario: Verify test isolation
    Given all cross-tenant tests are passing
    When we run tests in random order:
      - Use Maven surefire randomization
      - Run tests multiple times
      - Verify no test-pollution (one test affecting another)
    Then test isolation is verified
    And tests can run safely in parallel

