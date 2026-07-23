package com.gsserver.ui.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultProxyServiceTest {

  @Test
  void applyProxyConfigStoresLatestStateAndReturnsAccepted() {
    DefaultProxyService service = new DefaultProxyService(new InMemoryProxyOperationStateStore());

    ProxyResponse response =
        service.applyProxyConfig(
            new ProxyRequest("tenant-a", "ui-operator", true, "api.internal.local", 8443, true));

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message()).isEqualTo("Proxy configuration accepted");

    Optional<ProxyOperationState> latest = service.getLatestOperationState();
    assertThat(latest).isPresent();
    assertThat(latest.get().status()).isEqualTo("succeeded");
    assertThat(latest.get().enabled()).isTrue();
    assertThat(latest.get().upstreamHost()).isEqualTo("api.internal.local");
    assertThat(latest.get().upstreamPort()).isEqualTo(8443);
    assertThat(latest.get().tlsEnabled()).isTrue();
  }

  @Test
  void applyProxyConfigRejectsInvalidPort() {
    DefaultProxyService service = new DefaultProxyService(new InMemoryProxyOperationStateStore());

    assertThatThrownBy(
            () ->
                service.applyProxyConfig(
                    new ProxyRequest("tenant-a", "ui-operator", true, "api.internal.local", 70000, true)))
        .hasMessage("upstreamPort is out of range.");
  }
}
