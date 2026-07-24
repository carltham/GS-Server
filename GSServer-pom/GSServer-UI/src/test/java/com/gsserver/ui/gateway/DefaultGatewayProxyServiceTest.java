package com.gsserver.ui.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gsserver.ui.gateway.adapter.NginxCommandExecutor;
import com.gsserver.ui.gateway.adapter.NginxExecutionResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultGatewayProxyServiceTest {

  @Test
  void configureNginxProxyStoresLatestStateAndReturnsAccepted() {
    var executor = mock(NginxCommandExecutor.class);
    when(executor.configure(any()))
        .thenReturn(
            new NginxExecutionResult(
                true,
                "Nginx proxy configuration applied successfully",
                "/var/nginx-backups/config-12345.conf"));

    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(
            new InMemoryGatewayProxyOperationStateStore(), executor);

    GatewayProxyResponse response =
        service.configureNginxProxy(
            new GatewayProxyRequest(
                "tenant-a",
                "ui-operator",
                "api.internal.local",
                8443,
                true,
                "/etc/nginx/certs/server.crt",
                "/etc/nginx/certs/server.key"));

    assertThat(response.status()).isEqualTo("success");
    assertThat(response.message())
        .isEqualTo("Nginx proxy configuration applied successfully");

    Optional<GatewayProxyOperationState> latest = service.getLatestOperationState();
    assertThat(latest).isPresent();
    assertThat(latest.get().status()).isEqualTo("success");
    assertThat(latest.get().upstreamHost()).isEqualTo("api.internal.local");
    assertThat(latest.get().upstreamPort()).isEqualTo(8443);
    assertThat(latest.get().tlsEnabled()).isTrue();
  }

  @Test
  void configureNginxProxyRejectsInvalidPort() {
    var executor = mock(NginxCommandExecutor.class);
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(
            new InMemoryGatewayProxyOperationStateStore(), executor);

    assertThatThrownBy(
            () ->
                service.configureNginxProxy(
                    new GatewayProxyRequest(
                        "tenant-a",
                        "ui-operator",
                        "api.internal.local",
                        70000,
                        true,
                        "/etc/nginx/certs/server.crt",
                        "/etc/nginx/certs/server.key")))
        .hasMessage("upstreamPort is out of range.");
  }

  @Test
  void configureNginxProxyRequiresTlsCertWhenTlsEnabled() {
    var executor = mock(NginxCommandExecutor.class);
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(
            new InMemoryGatewayProxyOperationStateStore(), executor);

    assertThatThrownBy(
            () ->
                service.configureNginxProxy(
                    new GatewayProxyRequest(
                        "tenant-a",
                        "ui-operator",
                        "api.internal.local",
                        8443,
                        true,
                        null,
                        "/etc/nginx/certs/server.key")))
        .hasMessage("tlsCertPath is required when tlsEnabled is true.");
  }

  @Test
  void rollbackStoresNewStateAndReturnsAccepted() {
    var executor = mock(NginxCommandExecutor.class);
    when(executor.configure(any()))
        .thenReturn(
            new NginxExecutionResult(
                true,
                "Nginx proxy configuration applied successfully",
                "/var/nginx-backups/config-12345.conf"));
    when(executor.rollback(any()))
        .thenReturn(
            new NginxExecutionResult(
                true, "Nginx proxy configuration rolled back successfully", null));

    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(
            new InMemoryGatewayProxyOperationStateStore(), executor);

    // Create initial state
    GatewayProxyResponse response1 =
        service.configureNginxProxy(
            new GatewayProxyRequest(
                "tenant-a",
                "ui-operator",
                "api.internal.local",
                8443,
                true,
                "/etc/nginx/certs/server.crt",
                "/etc/nginx/certs/server.key"));

    Optional<GatewayProxyOperationState> latest = service.getLatestOperationState();
    String firstOperationId = latest.get().operationId();

    // Create second state
    service.configureNginxProxy(
        new GatewayProxyRequest(
            "tenant-a",
            "ui-operator",
            "api2.internal.local",
            8444,
            true,
            "/etc/nginx/certs/server.crt",
            "/etc/nginx/certs/server.key"));

    // Rollback to first state
    GatewayProxyResponse rollbackResponse = service.rollbackToState(firstOperationId);

    assertThat(rollbackResponse.status()).isEqualTo("rollback_success");
    assertThat(rollbackResponse.message())
        .isEqualTo("Nginx proxy configuration rolled back successfully");

    Optional<GatewayProxyOperationState> latestAfterRollback = service.getLatestOperationState();
    assertThat(latestAfterRollback).isPresent();
    assertThat(latestAfterRollback.get().status()).isEqualTo("rollback_success");
  }
}
