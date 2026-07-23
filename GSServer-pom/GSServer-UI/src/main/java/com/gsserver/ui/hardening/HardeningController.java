package com.gsserver.ui.hardening;

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
  public ResponseEntity<HardeningResponse> triggerHardening(@RequestBody HardeningRequest request) {
    HardeningResponse response = hardeningService.triggerHardening(request);
    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/latest")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_AUDIT_READERS')")
  public ResponseEntity<HardeningOperationState> latestHardeningOperationState() {
    return ResponseEntity.of(hardeningService.getLatestOperationState());
  }
}
