package com.gsserver.ui.security;

import java.util.List;

/**
 * A runtime-managed user persisted to the JSON user store.
 *
 * @param username unique login name
 * @param passwordHash encoded password (e.g. {@code {bcrypt}...}); never the plaintext
 * @param authorities granted group authorities (e.g. {@code GROUP_HARDENING_ADMINS})
 * @param enabled whether the account may log in
 */
public record ManagedUser(
    String username, String passwordHash, List<String> authorities, boolean enabled) {

  public ManagedUser {
    authorities = authorities == null ? List.of() : List.copyOf(authorities);
  }

  public ManagedUser withPasswordHash(String newHash) {
    return new ManagedUser(username, newHash, authorities, enabled);
  }

  public ManagedUser withAuthorities(List<String> newAuthorities) {
    return new ManagedUser(username, passwordHash, newAuthorities, enabled);
  }

  public ManagedUser withEnabled(boolean newEnabled) {
    return new ManagedUser(username, passwordHash, authorities, newEnabled);
  }
}
