package com.gsserver.ui.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultGatewayProxyServiceTest {

  @Test
  void configureNginxProxyStoresLatestStateAndReturnsAccepted() {
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(new InMemoryGatewayProxyOperationStateStore());

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

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message())
        .isEqualTo("Nginx proxy configuration accepted and queued for execution");

    Optional<GatewayProxyOperationState> latest = service.getLatestOperationState();
    assertThat(latest).isPresent();
    assertThat(latest.get().status()).isEqualTo("accepted");
    assertThat(latest.get().upstreamHost()).isEqualTo("api.internal.local");
    assertThat(latest.get().upstreamPort()).isEqualTo(8443);
    assertThat(latest.get().tlsEnabled()).isTrue();
  }

  @Test
  void configureNginxProxyRejectsInvalidPort() {
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(new InMemoryGatewayProxyOperationStateStore());

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
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(new InMemoryGatewayProxyOperationStateStore());

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
    DefaultGatewayProxyService service =
        new DefaultGatewayProxyService(new InMemoryGatewayProxyOperationStateStore());

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

    assertThat(rollbackResponse.status()).isEqualTo("rollback_accepted");
    assertThat(rollbackResponse.message()).isEqualTo("Rollback accepted and queued for execution");

    Optional<GatewayProxyOperationState> latestAfterRollback = service.getLatestOperationState();
    assertThat(latestAfterRollback).isPresent();
    assertThat(latestAfterRollback.get().status()).isEqualTo("rollback_accepted");
  }
}
