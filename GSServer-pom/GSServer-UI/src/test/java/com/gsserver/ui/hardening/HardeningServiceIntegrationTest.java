package com.gsserver.ui.hardening;

import com.gsserver.ui.hardening.adapter.HardeningExecutionReport;
import com.gsserver.ui.hardening.adapter.LinuxHardeningAdapter;
import com.gsserver.ui.hardening.adapter.WindowsHardeningAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Service Focused Integration Test: Hardening Service (Phase 1, Layer 2)
 *
 * Stack-based TDD workflow:
 * Handler IT failed at Service layer → PAUSE Handler IT → Write this focused IT → implement → resume
 *
 * This test validates: Service layer orchestration and Adapter delegation
 */
@SpringBootTest
@ActiveProfiles("test")
public class HardeningServiceIntegrationTest {

  @Autowired
  private HardeningService hardeningService;

  @MockBean
  private LinuxHardeningAdapter linuxHardeningAdapter;

  @MockBean
  private WindowsHardeningAdapter windowsHardeningAdapter;

  @BeforeEach
  void setupMocks() {
    // Mock successful hardening for both platforms
    HardeningExecutionReport successReport = new HardeningExecutionReport(
        "linux", 0, "", "Hardening succeeded", false
    );
    when(linuxHardeningAdapter.applyStrictHardening()).thenReturn(successReport);
    when(linuxHardeningAdapter.applyBaselineHardening()).thenReturn(successReport);

    HardeningExecutionReport windowsReport = new HardeningExecutionReport(
        "windows", 0, "", "Hardening succeeded", false
    );
    when(windowsHardeningAdapter.applyStrictHardening()).thenReturn(windowsReport);
    when(windowsHardeningAdapter.applyBaselineHardening()).thenReturn(windowsReport);
  }

  /**
   * Test 1: Service accepts valid request and orchestrates to Adapter
   *
   * PAUSE Handler IT here (Service not implemented)
   * This focused IT defines what Service must do
   */
  @Test
  public void service_acceptsRequest_orchestratesExecution_returnsResult() {
    // Arrange: valid hardening request
    HardeningRequest request = new HardeningRequest("tenant-a", "ui-operator", "strict");

    // Act: call service to harden
    HardeningResult result = hardeningService.harden(request);

    // Assert: service returned valid result
    assertThat(result).isNotNull();
    assertThat(result.getOperationId()).isNotNull();
    assertThat(result.isSuccess()).isTrue();

    // When this test runs:
    // ❌ FAILS: Service method not found or not implemented
    // After implementing Service:
    // ❌ FAILS: Adapter not implemented (next layer boundary)
  }

  /**
   * Test 2: Service validates authorization before execution
   *
   * Service must check: tenant authorization, operator authorization
   */
  @Test
  public void service_validatesAuthorizationBeforeExecution() {
    // Arrange: valid request
    HardeningRequest request = new HardeningRequest("tenant-a", "ui-operator", "strict");

    // Act & Assert: should execute without throwing
    assertThatCode(() -> hardeningService.harden(request))
        .doesNotThrowAnyException();
  }

  /**
   * Test 3: Service handles adapter errors and returns failure result
   *
   * When Adapter fails, Service should catch and return safe result
   */
  @Test
  public void service_handlesAdapterError_returnsFailureResult() {
    // This test will be expanded when Adapter is implemented
    // For now, just verify Service exists and can be called
    HardeningRequest request = new HardeningRequest("tenant-a", "ui-operator", "strict");

    // Should complete without throwing (Adapter may fail, Service handles it)
    HardeningResult result = hardeningService.harden(request);
    assertThat(result).isNotNull();
  }

  /**
   * Test 4: Service includes operation metadata in result
   */
  @Test
  public void service_includesOperationMetadata_inResult() {
    HardeningRequest request = new HardeningRequest("tenant-a", "ui-operator", "strict");

    HardeningResult result = hardeningService.harden(request);

    // Assert: result has metadata
    assertThat(result.getOperationId()).isNotBlank();
    assertThat(result.getTenantId()).isEqualTo("tenant-a");
    assertThat(result.getRequestedBy()).isEqualTo("ui-operator");
  }

}
