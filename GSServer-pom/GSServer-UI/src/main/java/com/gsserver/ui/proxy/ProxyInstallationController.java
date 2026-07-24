package com.gsserver.ui.proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proxy/install")
public class ProxyInstallationController {
  private final ProxyInstallationService proxyInstallationService;

  public ProxyInstallationController(ProxyInstallationService proxyInstallationService) {
    this.proxyInstallationService = proxyInstallationService;
  }

  @GetMapping("/guide")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<ProxyInstallGuideResponse> guide() {
    return ResponseEntity.ok(proxyInstallationService.getInstallGuide());
  }

  @GetMapping("/site-file")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<SiteFileResponse> siteFile() {
    return ResponseEntity.ok(proxyInstallationService.getSiteFile());
  }
}
