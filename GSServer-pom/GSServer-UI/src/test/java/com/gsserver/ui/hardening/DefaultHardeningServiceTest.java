package com.gsserver.ui.hardening;

import com.gsserver.ui.hardening.adapter.HardeningExecutionReport;
import com.gsserver.ui.hardening.adapter.LinuxHardeningAdapter;
import com.gsserver.ui.hardening.adapter.WindowsHardeningAdapter;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class DefaultHardeningServiceTest {

  @Test
  void triggerHardeningReturnsAcceptedResponse() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("linux", 0, "ok", "", false));

    HardeningOperationStateStore stateStore = new InMemoryHardeningOperationStateStore();

    DefaultHardeningService service =
      new DefaultHardeningService(
        linuxAdapter,
        windowsAdapter,
        stateStore,
        () -> DefaultHardeningService.ExecutionPlatform.LINUX);

    HardeningResponse response =
        service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "baseline"));

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message()).isEqualTo("Hardening request accepted");
    Optional<HardeningOperationState> latest = service.getLatestOperationState();
    assertThat(latest).isPresent();
    assertThat(latest.get().operationId()).isNotBlank();
    assertThatCode(() -> Instant.parse(latest.get().occurredAtUtc())).doesNotThrowAnyException();
    assertThat(latest.get().status()).isEqualTo("succeeded");
    assertThat(latest.get().rollbackStatus()).isEqualTo("not-required");
    verify(linuxAdapter, never()).rollbackBaselineHardening();
  }

    @Test
    void triggerHardeningUsesStrictPathOnLinux() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyStrictHardening())
      .thenReturn(new HardeningExecutionReport("linux", 0, "strict ok", "", false));

    DefaultHardeningService service =
      new DefaultHardeningService(
        linuxAdapter,
        windowsAdapter,
        new InMemoryHardeningOperationStateStore(),
        () -> DefaultHardeningService.ExecutionPlatform.LINUX);

    HardeningResponse response =
      service.triggerHardening(new HardeningRequest("tenant-a", "ui-admin", "strict"));

    assertThat(response.status()).isEqualTo("accepted");
    verify(linuxAdapter).applyStrictHardening();
    verify(windowsAdapter, never()).applyStrictHardening();
    }

    @Test
    void triggerHardeningUsesStrictPathOnWindows() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(windowsAdapter.applyStrictHardening())
      .thenReturn(new HardeningExecutionReport("windows", 0, "strict ok", "", false));

    DefaultHardeningService service =
      new DefaultHardeningService(
        linuxAdapter,
        windowsAdapter,
        new InMemoryHardeningOperationStateStore(),
        () -> DefaultHardeningService.ExecutionPlatform.WINDOWS);

    HardeningResponse response =
      service.triggerHardening(new HardeningRequest("tenant-a", "ui-admin", "strict"));

    assertThat(response.status()).isEqualTo("accepted");
    verify(windowsAdapter).applyStrictHardening();
    verify(linuxAdapter, never()).applyStrictHardening();
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
    when(linuxAdapter.rollbackBaselineHardening())
      .thenReturn(new HardeningExecutionReport("linux", 0, "rolled back", "", false));

    HardeningOperationStateStore stateStore = new InMemoryHardeningOperationStateStore();

    DefaultHardeningService service =
      new DefaultHardeningService(
        linuxAdapter,
        windowsAdapter,
        stateStore,
        () -> DefaultHardeningService.ExecutionPlatform.LINUX);

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "baseline")))
        .isInstanceOf(HardeningExecutionException.class)
      .hasMessageContaining("Hardening execution failed on linux")
      .hasMessageContaining("rollback succeeded");
    Optional<HardeningOperationState> latest = service.getLatestOperationState();
    assertThat(latest).isPresent();
    assertThat(latest.get().operationId()).isNotBlank();
    assertThatCode(() -> Instant.parse(latest.get().occurredAtUtc())).doesNotThrowAnyException();
    assertThat(latest.get().status()).isEqualTo("failed");
    assertThat(latest.get().rollbackStatus()).isEqualTo("succeeded");
    verify(linuxAdapter).rollbackBaselineHardening();
  }

  private DefaultHardeningService serviceWithSuccessfulAdapters() {
    LinuxHardeningAdapter linuxAdapter = mock(LinuxHardeningAdapter.class);
    WindowsHardeningAdapter windowsAdapter = mock(WindowsHardeningAdapter.class);
    when(linuxAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("linux", 0, "ok", "", false));
    when(linuxAdapter.applyStrictHardening())
      .thenReturn(new HardeningExecutionReport("linux", 0, "strict ok", "", false));
    when(windowsAdapter.applyBaselineHardening())
        .thenReturn(new HardeningExecutionReport("windows", 0, "ok", "", false));
    when(windowsAdapter.applyStrictHardening())
      .thenReturn(new HardeningExecutionReport("windows", 0, "strict ok", "", false));
    return new DefaultHardeningService(
      linuxAdapter,
      windowsAdapter,
      new InMemoryHardeningOperationStateStore(),
      () -> DefaultHardeningService.ExecutionPlatform.LINUX);
  }
}
