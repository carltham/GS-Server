package com.gsserver.ui.hardening;

import com.gsserver.ui.hardening.adapter.HardeningExecutionReport;
import com.gsserver.ui.hardening.adapter.LinuxHardeningAdapter;
import com.gsserver.ui.hardening.adapter.WindowsHardeningAdapter;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class DefaultHardeningService implements HardeningService {
  enum ExecutionPlatform {
    LINUX,
    WINDOWS
  }

  private static final Set<String> ALLOWED_TENANTS = Set.of("tenant-a", "tenant-b");
  private static final Set<String> ALLOWED_OPERATORS = Set.of("ui-operator", "ui-admin");
  private static final Set<String> ALLOWED_PROFILES = Set.of("baseline", "strict");
  private final LinuxHardeningAdapter linuxHardeningAdapter;
  private final WindowsHardeningAdapter windowsHardeningAdapter;
  private final HardeningOperationStateStore operationStateStore;
  private final Supplier<ExecutionPlatform> platformSupplier;

  @Autowired
  public DefaultHardeningService(
      LinuxHardeningAdapter linuxHardeningAdapter,
      WindowsHardeningAdapter windowsHardeningAdapter,
      HardeningOperationStateStore operationStateStore) {
    this(
        linuxHardeningAdapter,
        windowsHardeningAdapter,
        operationStateStore,
        DefaultHardeningService::detectPlatform);
  }

  DefaultHardeningService(
      LinuxHardeningAdapter linuxHardeningAdapter,
      WindowsHardeningAdapter windowsHardeningAdapter,
      HardeningOperationStateStore operationStateStore,
      Supplier<ExecutionPlatform> platformSupplier) {
    this.linuxHardeningAdapter = linuxHardeningAdapter;
    this.windowsHardeningAdapter = windowsHardeningAdapter;
    this.operationStateStore = operationStateStore;
    this.platformSupplier = platformSupplier;
  }

  @Override
  public HardeningResponse triggerHardening(HardeningRequest request) {
    validateRequest(request);
    String operationId = UUID.randomUUID().toString();
    String occurredAtUtc = Instant.now().toString();
    HardeningExecutionReport report = executeByProfile(request.profile());
    if (!report.successful()) {
      HardeningExecutionReport rollbackReport = rollbackByProfile(request.profile());
      String rollbackMessage = rollbackReport.successful() ? "rollback succeeded" : "rollback failed";
      operationStateStore.save(
          new HardeningOperationState(
              operationId,
              occurredAtUtc,
              "failed",
              request.tenantId(),
              request.requestedBy(),
              request.profile(),
              report.platform(),
              report.exitCode(),
              report.timedOut(),
              rollbackReport.successful() ? "succeeded" : "failed",
              report.stderr()));
      throw new HardeningExecutionException(
          "Hardening execution failed on "
              + report.platform()
              + ": "
              + report.stderr()
              + " ("
              + rollbackMessage
              + ")");
    }
    operationStateStore.save(
        new HardeningOperationState(
        operationId,
        occurredAtUtc,
            "succeeded",
            request.tenantId(),
            request.requestedBy(),
            request.profile(),
            report.platform(),
            report.exitCode(),
            report.timedOut(),
            "not-required",
            "Hardening request accepted"));
    return new HardeningResponse("accepted", "Hardening request accepted");
  }

  @Override
  public Optional<HardeningOperationState> getLatestOperationState() {
    return operationStateStore.getLatest();
  }

  private HardeningExecutionReport executeByProfile(String profile) {
    if (platformSupplier.get() == ExecutionPlatform.WINDOWS) {
      return executeOnWindows(profile);
    }
    return executeOnLinux(profile);
  }

  private HardeningExecutionReport rollbackByProfile(String profile) {
    if (platformSupplier.get() == ExecutionPlatform.WINDOWS) {
      return rollbackOnWindows(profile);
    }
    return rollbackOnLinux(profile);
  }

  private HardeningExecutionReport executeOnLinux(String profile) {
    if ("strict".equals(profile)) {
      return linuxHardeningAdapter.applyStrictHardening();
    }
    return linuxHardeningAdapter.applyBaselineHardening();
  }

  private HardeningExecutionReport rollbackOnLinux(String profile) {
    if ("strict".equals(profile)) {
      return linuxHardeningAdapter.rollbackStrictHardening();
    }
    return linuxHardeningAdapter.rollbackBaselineHardening();
  }

  private HardeningExecutionReport executeOnWindows(String profile) {
    if ("strict".equals(profile)) {
      return windowsHardeningAdapter.applyStrictHardening();
    }
    return windowsHardeningAdapter.applyBaselineHardening();
  }

  private HardeningExecutionReport rollbackOnWindows(String profile) {
    if ("strict".equals(profile)) {
      return windowsHardeningAdapter.rollbackStrictHardening();
    }
    return windowsHardeningAdapter.rollbackBaselineHardening();
  }

  private static ExecutionPlatform detectPlatform() {
    String osName = System.getProperty("os.name", "").toLowerCase();
    return osName.contains("win") ? ExecutionPlatform.WINDOWS : ExecutionPlatform.LINUX;
  }

  private void validateRequest(HardeningRequest request) {
    if (request == null) {
      throw new PolicyViolationException("Hardening request is required.");
    }

    if (isBlank(request.tenantId())) {
      throw new PolicyViolationException("tenantId is required.");
    }

    if (isBlank(request.requestedBy())) {
      throw new PolicyViolationException("requestedBy is required.");
    }

    if (isBlank(request.profile())) {
      throw new PolicyViolationException("profile is required.");
    }

    if (!ALLOWED_TENANTS.contains(request.tenantId())) {
      throw new PolicyViolationException("tenantId is not authorized.");
    }

    if (!ALLOWED_OPERATORS.contains(request.requestedBy())) {
      throw new PolicyViolationException("requestedBy is not authorized.");
    }

    if (!ALLOWED_PROFILES.contains(request.profile())) {
      throw new PolicyViolationException("profile is not supported.");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
