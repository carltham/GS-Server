package com.gsserver.ui.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DefaultHardeningServiceTest {

  @Test
  void triggerHardeningReturnsAcceptedResponse() {
    DefaultHardeningService service = new DefaultHardeningService();

    HardeningResponse response =
        service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "baseline"));

    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.message()).isEqualTo("Hardening request accepted");
  }

  @Test
  void triggerHardeningRejectsBlankTenantId() {
    DefaultHardeningService service = new DefaultHardeningService();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("", "ui-operator", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("tenantId is required.");
  }

  @Test
  void triggerHardeningRejectsBlankRequestedBy() {
    DefaultHardeningService service = new DefaultHardeningService();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "  ", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("requestedBy is required.");
  }

  @Test
  void triggerHardeningRejectsUnauthorizedTenantId() {
    DefaultHardeningService service = new DefaultHardeningService();

    assertThatThrownBy(
            () ->
                service.triggerHardening(
                    new HardeningRequest("tenant-unknown", "ui-operator", "baseline")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("tenantId is not authorized.");
  }

  @Test
  void triggerHardeningRejectsUnsupportedProfile() {
    DefaultHardeningService service = new DefaultHardeningService();

    assertThatThrownBy(
            () -> service.triggerHardening(new HardeningRequest("tenant-a", "ui-operator", "custom")))
        .isInstanceOf(PolicyViolationException.class)
        .hasMessage("profile is not supported.");
  }
}
