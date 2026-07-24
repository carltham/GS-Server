package com.gsserver.ui.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Authenticates against the {@link JsonUserStore}, so runtime user-management changes take effect
 * immediately without a restart. Registered as a bean by {@link SecurityConfig}.
 */
public class JsonUserDetailsService implements UserDetailsService {

  private final JsonUserStore userStore;

  public JsonUserDetailsService(JsonUserStore userStore) {
    this.userStore = userStore;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    ManagedUser user =
        userStore
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));

    return User.withUsername(user.username())
        .password(user.passwordHash())
        .disabled(!user.enabled())
        .authorities(user.authorities().toArray(new String[0]))
        .build();
  }
}
