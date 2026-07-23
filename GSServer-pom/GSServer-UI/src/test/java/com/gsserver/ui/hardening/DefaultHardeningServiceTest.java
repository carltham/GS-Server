package com.gsserver.ui.hardening;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultHardeningServiceTest {

  @Test
  void triggerHardeningReturnsAcceptedResponse() {
    DefaultHardeningService service = new DefaultHardeningService();

    HardeningResponse response = service.triggerHardening(new HardeningRequest());

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message()).isEqualTo("Hardening request accepted");
  }
}
