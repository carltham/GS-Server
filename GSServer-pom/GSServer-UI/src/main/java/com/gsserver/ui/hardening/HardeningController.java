package com.gsserver.ui.hardening;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hardening")
public class HardeningController {
  private final HardeningService hardeningService;

  public HardeningController(HardeningService hardeningService) {
    this.hardeningService = hardeningService;
  }

  @PostMapping
  public ResponseEntity<HardeningResponse> triggerHardening(@RequestBody HardeningRequest request) {
    HardeningResponse response = hardeningService.triggerHardening(request);
    return ResponseEntity.accepted().body(response);
  }
}
