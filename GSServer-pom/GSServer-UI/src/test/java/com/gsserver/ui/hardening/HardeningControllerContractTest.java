package com.gsserver.ui.hardening;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gsserver.ui.api.ApiExceptionHandler;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HardeningController.class)
@Import(ApiExceptionHandler.class)
class HardeningControllerContractTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private HardeningService hardeningService;

  @Test
  void triggerHardeningReturnsAcceptedContract() throws Exception {
    when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenReturn(new HardeningResponse("accepted", "Hardening request accepted"));

    mockMvc
      .perform(
        post("/api/v1/hardening")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{" +
            "\"tenantId\":\"tenant-a\"," +
            "\"requestedBy\":\"ui-operator\"," +
            "\"profile\":\"baseline\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("accepted"))
        .andExpect(jsonPath("$.message").value("Hardening request accepted"));

    verify(hardeningService).triggerHardening(any(HardeningRequest.class));
  }

    @Test
    void triggerHardeningReturnsStructuredErrorOnPolicyViolation() throws Exception {
    when(hardeningService.triggerHardening(any(HardeningRequest.class)))
      .thenThrow(new PolicyViolationException("tenantId is required."));

    mockMvc
      .perform(
        post("/api/v1/hardening")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{" +
            "\"tenantId\":\"\"," +
            "\"requestedBy\":\"ui-operator\"," +
            "\"profile\":\"baseline\"}"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"))
      .andExpect(jsonPath("$.message").value("tenantId is required."));
      }

      @Test
      void triggerHardeningReturnsStructuredErrorOnUnauthorizedTenant() throws Exception {
      when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenThrow(new PolicyViolationException("tenantId is not authorized."));

      mockMvc
        .perform(
          post("/api/v1/hardening")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{"
                + "\"tenantId\":\"tenant-unknown\","
                + "\"requestedBy\":\"ui-operator\","
                + "\"profile\":\"baseline\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"))
        .andExpect(jsonPath("$.message").value("tenantId is not authorized."));
      }

      @Test
      void triggerHardeningReturnsStructuredErrorOnUnauthorizedOperator() throws Exception {
      when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenThrow(new PolicyViolationException("requestedBy is not authorized."));

      mockMvc
        .perform(
          post("/api/v1/hardening")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{"
                + "\"tenantId\":\"tenant-a\","
                + "\"requestedBy\":\"guest\","
                + "\"profile\":\"baseline\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"))
        .andExpect(jsonPath("$.message").value("requestedBy is not authorized."));
      }

      @Test
      void triggerHardeningReturnsStructuredErrorOnUnsupportedProfile() throws Exception {
      when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenThrow(new PolicyViolationException("profile is not supported."));

      mockMvc
        .perform(
          post("/api/v1/hardening")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{"
                + "\"tenantId\":\"tenant-a\","
                + "\"requestedBy\":\"ui-operator\","
                + "\"profile\":\"custom\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"))
        .andExpect(jsonPath("$.message").value("profile is not supported."));
      }

      @Test
      void triggerHardeningReturnsStructuredErrorOnExecutionFailure() throws Exception {
      when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenThrow(new HardeningExecutionException("Hardening execution failed on linux: denied (rollback succeeded)"));

      mockMvc
        .perform(
          post("/api/v1/hardening")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{"
                + "\"tenantId\":\"tenant-a\","
                + "\"requestedBy\":\"ui-operator\","
                + "\"profile\":\"baseline\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("EXECUTION_FAILED"))
        .andExpect(jsonPath("$.message").value("Hardening execution failed on linux: denied (rollback succeeded)"));
      }

      @Test
      void triggerHardeningReturnsStructuredErrorOnExecutionFailureWithRollbackFailure() throws Exception {
      when(hardeningService.triggerHardening(any(HardeningRequest.class)))
        .thenThrow(new HardeningExecutionException("Hardening execution failed on windows: denied (rollback failed)"));

      mockMvc
        .perform(
          post("/api/v1/hardening")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{"
                + "\"tenantId\":\"tenant-a\","
                + "\"requestedBy\":\"ui-admin\","
                + "\"profile\":\"strict\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("EXECUTION_FAILED"))
        .andExpect(jsonPath("$.message").value("Hardening execution failed on windows: denied (rollback failed)"));
      }

  @Test
  void triggerHardeningReturnsStructuredErrorOnMalformedRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/hardening")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("Request body is malformed."));

    verifyNoInteractions(hardeningService);
  }

  @Test
  void latestHardeningOperationStateReturnsCurrentState() throws Exception {
    when(hardeningService.getLatestOperationState())
        .thenReturn(
            Optional.of(
                new HardeningOperationState(
                  "operation-123",
                  "2026-07-23T14:00:00Z",
                    "succeeded",
                    "tenant-a",
                    "ui-operator",
                    "baseline",
                    "linux",
                    0,
                    false,
                    "not-required",
                    "Hardening request accepted")));

    mockMvc
        .perform(get("/api/v1/hardening/latest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operationId").value("operation-123"))
        .andExpect(jsonPath("$.occurredAtUtc").value("2026-07-23T14:00:00Z"))
        .andExpect(jsonPath("$.status").value("succeeded"))
        .andExpect(jsonPath("$.tenantId").value("tenant-a"))
        .andExpect(jsonPath("$.profile").value("baseline"))
        .andExpect(jsonPath("$.platform").value("linux"));
  }

  @Test
  void latestHardeningOperationStateReturnsNotFoundWhenNoState() throws Exception {
    when(hardeningService.getLatestOperationState()).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/hardening/latest"))
        .andExpect(status().isNotFound());
  }
}
