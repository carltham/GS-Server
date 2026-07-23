package com.gsserver.ui.hardening;

import com.gsserver.ui.hardening.adapter.HardeningExecutionReport;
import com.gsserver.ui.hardening.adapter.LinuxHardeningAdapter;
import com.gsserver.ui.hardening.adapter.WindowsHardeningAdapter;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class DefaultHardeningService implements HardeningService {
  private static final Set<String> ALLOWED_TENANTS = Set.of("tenant-a", "tenant-b");
  private static final Set<String> ALLOWED_OPERATORS = Set.of("ui-operator", "ui-admin");
  private static final Set<String> ALLOWED_PROFILES = Set.of("baseline", "strict");
  private final LinuxHardeningAdapter linuxHardeningAdapter;
  private final WindowsHardeningAdapter windowsHardeningAdapter;

  public DefaultHardeningService(
      LinuxHardeningAdapter linuxHardeningAdapter,
      WindowsHardeningAdapter windowsHardeningAdapter) {
    this.linuxHardeningAdapter = linuxHardeningAdapter;
    this.windowsHardeningAdapter = windowsHardeningAdapter;
  }

  @Override
  public HardeningResponse triggerHardening(HardeningRequest request) {
    validateRequest(request);
    HardeningExecutionReport report = executeByProfile(request.profile());
    if (!report.successful()) {
      throw new HardeningExecutionException(
          "Hardening execution failed on " + report.platform() + ": " + report.stderr());
    }
    return new HardeningResponse("accepted", "Hardening request accepted");
  }

  private HardeningExecutionReport executeByProfile(String profile) {
    if ("strict".equals(profile)) {
      return windowsHardeningAdapter.applyBaselineHardening();
    }
    return linuxHardeningAdapter.applyBaselineHardening();
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
