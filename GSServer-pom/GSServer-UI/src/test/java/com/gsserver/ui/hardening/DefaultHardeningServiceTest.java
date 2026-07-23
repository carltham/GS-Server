package com.gsserver.ui.hardening;

import com.gsserver.ui.hardening.adapter.HardeningExecutionReport;
import com.gsserver.ui.hardening.adapter.LinuxHardeningAdapter;
import com.gsserver.ui.hardening.adapter.WindowsHardeningAdapter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class DefaultHardeningServiceTest {

  @Test
  void triggerHardeningReturnsAcceptedResponse() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("linux", 0, "ok", "", false));

    DefaultHardeningService service = new DefaultHardeningService(linuxAdapter, windowsAdapter);

    HardeningResponse response =
        service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "baseline"));

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message()).isEqualTo("Hardening request accepted");
  }

  @Test
  void triggerHardeningRejectsBlankTenantId() {
    DefaultHardeningService service = serviceWithSuccessfulAdapters();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("", "ui-operator", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("tenantId is required.");
  }

  @Test
  void triggerHardeningRejectsBlankRequestedBy() {
    DefaultHardeningService service = serviceWithSuccessfulAdapters();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "  ", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("requestedBy is required.");
  }

  @Test
  void triggerHardeningRejectsUnauthorizedTenantId() {
    DefaultHardeningService service = serviceWithSuccessfulAdapters();

    assertThatThrownBy(
            () ->
                service.triggerHardening(
                    new HardeningRequest("tenant-unknown", "ui-operator", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("tenantId is not authorized.");
  }

  @Test
  void triggerHardeningRejectsUnsupportedProfile() {
    DefaultHardeningService service = serviceWithSuccessfulAdapters();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "custom")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("profile is not supported.");
  }

  @Test
  void triggerHardeningRaisesExecutionFailureWhenAdapterFails() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("linux", 32, "", "permission denied", false));

    DefaultHardeningService service = new DefaultHardeningService(linuxAdapter, windowsAdapter);

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "baseline")))
        .isInstanceOf(HardeningExecutionException.class)
        .hasMessageContaining("Hardening execution failed on linux");
  }

  private DefaultHardeningService serviceWithSuccessfulAdapters() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("linux", 0, "ok", "", false));
    when(windowsAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("windows", 0, "ok", "", false));
    return new DefaultHardeningService(linuxAdapter, windowsAdapter);
  }
}
