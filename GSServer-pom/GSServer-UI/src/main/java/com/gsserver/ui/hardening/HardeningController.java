package com.gsserver.ui.hardening;

import com.gsserver.ui.common.ApiSuccessResponse;
import com.gsserver.ui.common.CorrelationIdUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/hardening")
public class HardeningController {
  private final HardeningService hardeningService;

  public HardeningController(HardeningService hardeningService) {
    this.hardeningService = hardeningService;
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS')")
  public ResponseEntity<?> triggerHardening(@RequestBody HardeningRequest request) {
    // Generate correlation ID for this request (for tracing through layers)
    String correlationId = CorrelationIdUtil.generateCorrelationId();
    CorrelationIdUtil.setInMdc(correlationId);

    try {
      // Validate request
      if (request == null || request.tenantId() == null || request.tenantId().isEmpty()) {
        return ResponseEntity.unprocessableEntity().build();
      }

      HardeningResponse response = hardeningService.triggerHardening(request);
      return ResponseEntity.accepted().body(
          new ApiSuccessResponse(response, correlationId)
      );
    } finally {
      CorrelationIdUtil.clearFromMdc();
    }
  }

  @GetMapping("/latest")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_AUDIT_READERS')")
  public ResponseEntity<?> latestHardeningOperationState() {
    String correlationId = CorrelationIdUtil.generateCorrelationId();
    CorrelationIdUtil.setInMdc(correlationId);

    try {
      return ResponseEntity.of(
          hardeningService.getLatestOperationState()
              .map(state -> new ApiSuccessResponse(state, correlationId))
      );
    } finally {
      CorrelationIdUtil.clearFromMdc();
    }
  }
}
