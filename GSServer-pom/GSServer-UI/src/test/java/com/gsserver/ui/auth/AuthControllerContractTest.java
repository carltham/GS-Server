package com.gsserver.ui.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gsserver.ui.api.ApiExceptionHandler;
import com.gsserver.ui.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AuthControllerContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void meReturnsUnauthorizedWhenNoCredentials() throws Exception {
    mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void meReturnsIdentityForDemoUser() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/me").with(httpBasic("demo", "demo-pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("demo"))
        .andExpect(jsonPath("$.authorities[0]").value("GROUP_HARDENING_OPERATORS"));
  }

  @Test
  void meAllowsThorFromLocalhostWithoutPassword() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("thor"));
  }

  @Test
  void meRejectsThorFromRemoteAddress() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(httpBasic("thor", ""))
                .with(
                    request -> {
                      request.setRemoteAddr("10.10.10.10");
                      return request;
                    }))
        .andExpect(status().isUnauthorized());
  }
}
