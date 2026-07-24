package com.gsserver.ui.gateway;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultGatewayProxyService implements GatewayProxyService {
  private static final Set<String> ALLOWED_TENANTS = Set.of("tenant-a", "tenant-b");
  private static final Set<String> ALLOWED_OPERATORS = Set.of("ui-operator", "ui-admin", "thor", "demo");

  private final GatewayProxyOperationStateStore operationStateStore;

  public DefaultGatewayProxyService(GatewayProxyOperationStateStore operationStateStore) {
    this.operationStateStore = operationStateStore;
  }

  @Override
  public GatewayProxyResponse configureNginxProxy(GatewayProxyRequest request) {
    validateRequest(request);

    String operationId = UUID.randomUUID().toString();
    String occurredAtUtc = Instant.now().toString();
    Optional<GatewayProxyOperationState> previousState = operationStateStore.getLatest();

    GatewayProxyOperationState state =
        new GatewayProxyOperationState(
            operationId,
            occurredAtUtc,
            "accepted",
            request.tenantId(),
            request.requestedBy(),
            request.upstreamHost(),
            request.upstreamPort(),
            request.tlsEnabled(),
            request.tlsCertPath(),
            request.tlsKeyPath(),
            previousState.map(GatewayProxyOperationState::operationId).orElse(null),
            "Nginx proxy configuration accepted and queued for execution");

    operationStateStore.save(state);
    return new GatewayProxyResponse("accepted", "Nginx proxy configuration accepted and queued for execution");
  }

  @Override
  public Optional<GatewayProxyOperationState> getLatestOperationState() {
    return operationStateStore.getLatest();
  }

  @Override
  public GatewayProxyResponse rollbackToState(String operationId) {
    Optional<GatewayProxyOperationState> previousState = operationStateStore.getById(operationId);

    if (previousState.isEmpty()) {
      throw new GatewayProxyExecutionException("Operation state not found for rollback: " + operationId);
    }

    GatewayProxyOperationState state = previousState.get();
    String newOperationId = UUID.randomUUID().toString();
    String occurredAtUtc = Instant.now().toString();

    GatewayProxyOperationState rollbackState =
        new GatewayProxyOperationState(
            newOperationId,
            occurredAtUtc,
            "rollback_accepted",
            state.tenantId(),
            state.requestedBy(),
            state.upstreamHost(),
            state.upstreamPort(),
            state.tlsEnabled(),
            state.tlsCertPath(),
            state.tlsKeyPath(),
            operationId,
            "Rollback to previous state accepted and queued for execution");

    operationStateStore.save(rollbackState);
    return new GatewayProxyResponse("rollback_accepted", "Rollback accepted and queued for execution");
  }

  private void validateRequest(GatewayProxyRequest request) {
    if (request == null) {
      throw new GatewayProxyExecutionException("Gateway proxy request is required.");
    }

    if (isBlank(request.tenantId())) {
      throw new GatewayProxyExecutionException("tenantId is required.");
    }

    if (isBlank(request.requestedBy())) {
      throw new GatewayProxyExecutionException("requestedBy is required.");
    }

    if (isBlank(request.upstreamHost())) {
      throw new GatewayProxyExecutionException("upstreamHost is required.");
    }

    if (!ALLOWED_TENANTS.contains(request.tenantId())) {
      throw new GatewayProxyExecutionException("tenantId is not authorized.");
    }

    if (!ALLOWED_OPERATORS.contains(request.requestedBy())) {
      throw new GatewayProxyExecutionException("requestedBy is not authorized.");
    }

    if (request.upstreamPort() < 1 || request.upstreamPort() > 65535) {
      throw new GatewayProxyExecutionException("upstreamPort is out of range.");
    }

    if (request.tlsEnabled()) {
      if (isBlank(request.tlsCertPath())) {
        throw new GatewayProxyExecutionException("tlsCertPath is required when tlsEnabled is true.");
      }
      if (isBlank(request.tlsKeyPath())) {
        throw new GatewayProxyExecutionException("tlsKeyPath is required when tlsEnabled is true.");
      }
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
