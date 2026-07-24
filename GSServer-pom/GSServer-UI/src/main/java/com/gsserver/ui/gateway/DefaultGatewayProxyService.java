package com.gsserver.ui.gateway;

import com.gsserver.ui.gateway.adapter.NginxCommandExecutor;
import com.gsserver.ui.gateway.adapter.NginxConfigurationCommand;
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
  private final NginxCommandExecutor nginxExecutor;

  public DefaultGatewayProxyService(
      GatewayProxyOperationStateStore operationStateStore, NginxCommandExecutor nginxExecutor) {
    this.operationStateStore = operationStateStore;
    this.nginxExecutor = nginxExecutor;
  }

  @Override
  public GatewayProxyResponse configureNginxProxy(GatewayProxyRequest request) {
    validateRequest(request);

    String operationId = UUID.randomUUID().toString();
    String occurredAtUtc = Instant.now().toString();
    Optional<GatewayProxyOperationState> previousState = operationStateStore.getLatest();

    NginxConfigurationCommand command =
        new NginxConfigurationCommand(
            request.upstreamHost(),
            request.upstreamPort(),
            request.tlsEnabled(),
            request.tlsCertPath(),
            request.tlsKeyPath());

    var executionResult = nginxExecutor.configure(command);

    String status = executionResult.success() ? "success" : "failed";
    GatewayProxyOperationState state =
        new GatewayProxyOperationState(
            operationId,
            occurredAtUtc,
            status,
            request.tenantId(),
            request.requestedBy(),
            request.upstreamHost(),
            request.upstreamPort(),
            request.tlsEnabled(),
            request.tlsCertPath(),
            request.tlsKeyPath(),
            previousState.map(GatewayProxyOperationState::operationId).orElse(null),
            executionResult.message());

    operationStateStore.save(state);
    return new GatewayProxyResponse(status, executionResult.message());
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

    var executionResult = nginxExecutor.rollback(state.tlsCertPath());

    String status = executionResult.success() ? "rollback_success" : "rollback_failed";
    GatewayProxyOperationState rollbackState =
        new GatewayProxyOperationState(
            newOperationId,
            occurredAtUtc,
            status,
            state.tenantId(),
            state.requestedBy(),
            state.upstreamHost(),
            state.upstreamPort(),
            state.tlsEnabled(),
            state.tlsCertPath(),
            state.tlsKeyPath(),
            operationId,
            executionResult.message());

    operationStateStore.save(rollbackState);
    return new GatewayProxyResponse(status, executionResult.message());
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
