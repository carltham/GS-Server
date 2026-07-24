package com.gsserver.ui.gateway;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway/proxy")
public class GatewayProxyController {
  private final GatewayProxyService gatewayProxyService;

  public GatewayProxyController(GatewayProxyService gatewayProxyService) {
    this.gatewayProxyService = gatewayProxyService;
  }

  @PostMapping("/configure")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<GatewayProxyResponse> configureNginxProxy(
      @RequestBody GatewayProxyRequest request) {
    GatewayProxyResponse response = gatewayProxyService.configureNginxProxy(request);
    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/latest")
  @PreAuthorize(
      "hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_AUDIT_READERS','GROUP_SUPERUSER')")
  public ResponseEntity<GatewayProxyOperationState> latestOperationState() {
    Optional<GatewayProxyOperationState> latest = gatewayProxyService.getLatestOperationState();
    return ResponseEntity.of(latest);
  }

  @PostMapping("/rollback/{operationId}")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<GatewayProxyResponse> rollback(@PathVariable String operationId) {
    GatewayProxyResponse response = gatewayProxyService.rollbackToState(operationId);
    return ResponseEntity.accepted().body(response);
  }
}
