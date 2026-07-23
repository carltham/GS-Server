package com.gsserver.ui.proxy;

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

@WebMvcTest(ProxyController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class})
class ProxyControllerContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ProxyService proxyService;

  @Test
  void applyProxyConfigReturnsAcceptedForThor() throws Exception {
    when(proxyService.applyProxyConfig(any(ProxyRequest.class)))
        .thenReturn(new ProxyResponse("accepted", "Proxy configuration accepted"));

    mockMvc
        .perform(
            post("/api/v1/proxy")
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
                        + "\"enabled\":true,"
                        + "\"upstreamHost\":\"api.internal.local\","
                        + "\"upstreamPort\":8443,"
                        + "\"tlsEnabled\":true}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("accepted"));
  }

  @Test
  void latestProxyStateReturnsStateForThor() throws Exception {
    when(proxyService.getLatestOperationState())
        .thenReturn(
            Optional.of(
                new ProxyOperationState(
                    "operation-1",
                    "2026-07-23T14:00:00Z",
                    "succeeded",
                    "tenant-a",
                    "thor",
                    true,
                    "api.internal.local",
                    8443,
                    true,
                    "Proxy configuration accepted")));

    mockMvc
        .perform(
            get("/api/v1/proxy/latest")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.upstreamHost").value("api.internal.local"))
        .andExpect(jsonPath("$.upstreamPort").value(8443))
        .andExpect(jsonPath("$.tlsEnabled").value(true));
  }
}
