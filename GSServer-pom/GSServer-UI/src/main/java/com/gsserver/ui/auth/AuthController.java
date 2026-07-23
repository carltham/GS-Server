package com.gsserver.ui.auth;

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
