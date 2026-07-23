package com.gsserver.ui.proxy;

import com.gsserver.ui.hardening.PolicyViolationException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultProxyService implements ProxyService {
  private static final Set<String> ALLOWED_TENANTS = Set.of("tenant-a", "tenant-b");
  private static final Set<String> ALLOWED_OPERATORS = Set.of("ui-operator", "ui-admin", "thor", "demo");

  private final ProxyOperationStateStore operationStateStore;

  public DefaultProxyService(ProxyOperationStateStore operationStateStore) {
    this.operationStateStore = operationStateStore;
  }

  @Override
  public ProxyResponse applyProxyConfig(ProxyRequest request) {
    validateRequest(request);

    String operationId = UUID.randomUUID().toString();
    String occurredAtUtc = Instant.now().toString();

    ProxyOperationState state =
        new ProxyOperationState(
            operationId,
            occurredAtUtc,
            "succeeded",
            request.tenantId(),
            request.requestedBy(),
            request.enabled(),
            request.upstreamHost(),
            request.upstreamPort(),
            request.tlsEnabled(),
            "Proxy configuration accepted");

    operationStateStore.save(state);
    return new ProxyResponse("accepted", "Proxy configuration accepted");
  }

  @Override
  public Optional<ProxyOperationState> getLatestOperationState() {
    return operationStateStore.getLatest();
  }

  @Override
  public ProxyRuntimeStatus getRuntimeStatus() {
    boolean nginxRunning = isProcessRunning("nginx");
    boolean apacheRunning = isProcessRunning("apache2") || isProcessRunning("httpd");
    String detectedServer = "none";

    if (nginxRunning) {
      detectedServer = "nginx";
    } else if (apacheRunning) {
      detectedServer = "apache";
    }

    return new ProxyRuntimeStatus(nginxRunning, apacheRunning, detectedServer);
  }

  private void validateRequest(ProxyRequest request) {
    if (request == null) {
      throw new PolicyViolationException("Proxy request is required.");
    }

    if (isBlank(request.tenantId())) {
      throw new PolicyViolationException("tenantId is required.");
    }

    if (isBlank(request.requestedBy())) {
      throw new PolicyViolationException("requestedBy is required.");
    }

    if (isBlank(request.upstreamHost())) {
      throw new PolicyViolationException("upstreamHost is required.");
    }

    if (!ALLOWED_TENANTS.contains(request.tenantId())) {
      throw new PolicyViolationException("tenantId is not authorized.");
    }

    if (!ALLOWED_OPERATORS.contains(request.requestedBy())) {
      throw new PolicyViolationException("requestedBy is not authorized.");
    }

    if (request.upstreamPort() < 1 || request.upstreamPort() > 65535) {
      throw new PolicyViolationException("upstreamPort is out of range.");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private boolean isProcessRunning(String token) {
    String loweredToken = token.toLowerCase(Locale.ROOT);
    return ProcessHandle.allProcesses()
        .map(ProcessHandle::info)
        .map(info -> info.command().orElse(""))
        .map(command -> command.toLowerCase(Locale.ROOT))
        .anyMatch(command -> command.contains(loweredToken));
  }
}
