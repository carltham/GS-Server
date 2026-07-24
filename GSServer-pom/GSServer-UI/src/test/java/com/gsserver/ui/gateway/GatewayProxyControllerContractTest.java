package com.gsserver.ui.gateway;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gsserver.ui.api.ApiExceptionHandler;
import com.gsserver.ui.security.SecurityConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GatewayProxyController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class})
class GatewayProxyControllerContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GatewayProxyService gatewayProxyService;

  @Test
  void configureNginxProxyReturnsAcceptedForThor() throws Exception {
    when(gatewayProxyService.configureNginxProxy(any(GatewayProxyRequest.class)))
        .thenReturn(
            new GatewayProxyResponse(
                "accepted", "Nginx proxy configuration accepted and queued for execution"));

    mockMvc
        .perform(
            post("/api/v1/gateway/proxy/configure")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"tenantId\":\"tenant-a\","
                        + "\"requestedBy\":\"thor\","
                        + "\"upstreamHost\":\"api.internal.local\","
                        + "\"upstreamPort\":8443,"
                        + "\"tlsEnabled\":true,"
                        + "\"tlsCertPath\":\"/etc/nginx/certs/server.crt\","
                        + "\"tlsKeyPath\":\"/etc/nginx/certs/server.key\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("accepted"));
  }

  @Test
  void latestOperationStateReturnsStateForThor() throws Exception {
    when(gatewayProxyService.getLatestOperationState())
        .thenReturn(
            Optional.of(
                new GatewayProxyOperationState(
                    "operation-1",
                    "2026-07-23T14:00:00Z",
                    "accepted",
                    "tenant-a",
                    "thor",
                    "api.internal.local",
                    8443,
                    true,
                    "/etc/nginx/certs/server.crt",
                    "/etc/nginx/certs/server.key",
                    null,
                    "Nginx proxy configuration accepted and queued for execution")));

    mockMvc
        .perform(
            get("/api/v1/gateway/proxy/latest")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.upstreamHost").value("api.internal.local"))
        .andExpect(jsonPath("$.upstreamPort").value(8443))
        .andExpect(jsonPath("$.tlsEnabled").value(true));
  }

  @Test
  void rollbackReturnsAcceptedForThor() throws Exception {
    when(gatewayProxyService.rollbackToState("operation-1"))
        .thenReturn(new GatewayProxyResponse("rollback_accepted", "Rollback accepted and queued for execution"));

    mockMvc
        .perform(
            post("/api/v1/gateway/proxy/rollback/operation-1")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("rollback_accepted"));
  }
}
