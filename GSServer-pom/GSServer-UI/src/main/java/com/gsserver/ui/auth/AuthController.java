package com.gsserver.ui.auth;

import com.gsserver.ui.security.RequestOriginUtils;
import com.gsserver.ui.security.SecurityUsersProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final SecurityUsersProperties securityUsersProperties;

  public AuthController(SecurityUsersProperties securityUsersProperties) {
    this.securityUsersProperties = securityUsersProperties;
  }

  /**
   * Public login-screen configuration. Thor is offered only when it is enabled AND the request
   * genuinely comes from localhost — so the button appears on the box but never over the proxy.
   */
  @GetMapping("/config")
  public AuthConfigResponse config(HttpServletRequest request) {
    boolean thorAvailable =
        securityUsersProperties.isThorLoginEnabled() && RequestOriginUtils.isLoopbackRequest(request);
    return new AuthConfigResponse(thorAvailable);
  }

  /** Feature flags relevant to the login screen. */
  public record AuthConfigResponse(boolean thorLoginEnabled) {}

  @GetMapping("/me")
  public ResponseEntity<AuthIdentityResponse> me(Authentication authentication) {
    List<String> authorities =
        authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .sorted(Comparator.naturalOrder())
            .toList();

    return ResponseEntity.ok(new AuthIdentityResponse(authentication.getName(), authorities));
  }
}
