package com.gsserver.ui.hardening;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gsserver.ui.api.ApiExceptionHandler;
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
        .perform(post("/api/v1/hardening").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("accepted"))
        .andExpect(jsonPath("$.message").value("Hardening request accepted"));

    verify(hardeningService).triggerHardening(any(HardeningRequest.class));
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
}
