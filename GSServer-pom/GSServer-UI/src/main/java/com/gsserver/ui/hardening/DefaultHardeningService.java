package com.gsserver.ui.hardening;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class DefaultHardeningService implements HardeningService {
  private static final Set<String> ALLOWED_TENANTS = Set.of("tenant-a", "tenant-b");
  private static final Set<String> ALLOWED_OPERATORS = Set.of("ui-operator", "ui-admin");
  private static final Set<String> ALLOWED_PROFILES = Set.of("baseline", "strict");

  @Override
  public HardeningResponse triggerHardening(HardeningRequest request) {
    validateRequest(request);
    return new HardeningResponse("accepted", "Hardening request accepted");
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
