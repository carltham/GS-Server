package com.gsserver.ui.proxy;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {
  private final ProxyService proxyService;

  public ProxyController(ProxyService proxyService) {
    this.proxyService = proxyService;
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<ProxyResponse> applyProxyConfig(@RequestBody ProxyRequest request) {
    ProxyResponse response = proxyService.applyProxyConfig(request);
    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/latest")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_AUDIT_READERS','GROUP_SUPERUSER')")
  public ResponseEntity<ProxyOperationState> latestProxyOperationState() {
    Optional<ProxyOperationState> latest = proxyService.getLatestOperationState();
    return ResponseEntity.of(latest);
  }

  @GetMapping("/runtime")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_AUDIT_READERS','GROUP_SUPERUSER')")
  public ResponseEntity<ProxyRuntimeStatus> runtimeStatus() {
    return ResponseEntity.ok(proxyService.getRuntimeStatus());
  }
}
