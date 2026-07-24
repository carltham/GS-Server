package com.gsserver.ui.hardening;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Handler Focused Integration Test: Secrets Redaction (Phase 1, Layer 1)
 *
 * Stack-based TDD workflow:
 * E2E test failed → PAUSE E2E → Write this focused IT → implement → resume E2E
 *
 * This test validates: Handler layer catches Service errors and redacts secrets in response
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HardeningHandlerSecretsRedactionIT {

  @Autowired
  private MockMvc mockMvc;

  /**
   * Test: Handler returns error with secrets REDACTED
   *
   * When Service throws exception with credentials,
   * Handler must catch it and return structured response without secrets
   */
  @Test
  @WithMockUser(authorities = "GROUP_HARDENING_OPERATORS")
  public void handler_catches_serviceError_redactsSecrets_returnsStructuredResponse() throws Exception {
    // Arrange: request that triggers error
    String requestBody = """
        {
          "tenantId": "tenant-a",
          "requestedBy": "test-user",
          "profile": "strict"
        }
        """;

    // Act: POST to handler endpoint
    mockMvc.perform(post("/api/v1/hardening")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))

        // Assert: error status
        .andExpect(status().is5xxServerError())

        // Assert: structured error response format (not raw stack trace)
        .andExpect(jsonPath("$.errorId").exists())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.correlationId").exists())
        .andExpect(jsonPath("$.severity").value("ERROR"))

        // Assert: secrets are REDACTED
        .andExpect(jsonPath("$.message", not(containsString("password"))))
        .andExpect(jsonPath("$.message", not(containsString("user:"))))
        .andExpect(jsonPath("$.message", not(containsString(".ssh"))))
        .andExpect(jsonPath("$.message", not(containsString("jdbc:"))))
        .andExpect(jsonPath("$.message", not(containsString("mysql://"))))

        // Assert: message is human-readable
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.message", notNullValue()));
  }

  /**
   * Test: Handler assigns correlationId to track requests
   */
  @Test
  @WithMockUser(authorities = "GROUP_HARDENING_OPERATORS")
  public void handler_assignsCorrelationId_forTracing() throws Exception {
    String requestBody = """
        {
          "serverId": "test-server"
        }
        """;

    mockMvc.perform(post("/api/v1/hardening")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))

        // Assert: response includes correlationId
        .andExpect(jsonPath("$.correlationId").exists())
        .andExpect(jsonPath("$.correlationId", matchesPattern("^[0-9a-f-]{36}$")));  // UUID format
  }

  /**
   * Test: Success response also includes correlationId
   */
  @Test
  @WithMockUser(authorities = "GROUP_HARDENING_OPERATORS")
  public void handler_includesCorrelationId_inSuccessResponse() throws Exception {
    String requestBody = """
        {
          "serverId": "valid-server"
        }
        """;

    mockMvc.perform(post("/api/v1/hardening")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))

        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.correlationId").exists());
  }

}
