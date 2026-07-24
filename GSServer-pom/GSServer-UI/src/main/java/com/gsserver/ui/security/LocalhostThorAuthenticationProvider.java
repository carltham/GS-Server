package com.gsserver.ui.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class LocalhostThorAuthenticationProvider extends DaoAuthenticationProvider {

  private static final String THOR_USERNAME = "thor";

  /** Master switch for passwordless thor login; the localhost origin check still gates every login. */
  private boolean thorLoginEnabled = true;

  public void setThorLoginEnabled(boolean thorLoginEnabled) {
    this.thorLoginEnabled = thorLoginEnabled;
  }

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
    if (!THOR_USERNAME.equals(userDetails.getUsername())) {
      super.additionalAuthenticationChecks(userDetails, authentication);
      return;
    }

    if (!thorLoginEnabled) {
      throw new BadCredentialsException("thor passwordless login is disabled.");
    }

    String presentedPassword =
        authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
    if (!presentedPassword.isEmpty()) {
      throw new BadCredentialsException("thor must authenticate without a password.");
    }

    if (!isLocalhostRequest()) {
      throw new BadCredentialsException("thor is restricted to localhost login.");
    }
  }

  private boolean isLocalhostRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
      return false;
    }
    return RequestOriginUtils.isLoopbackRequest(attributes.getRequest());
  }
}
