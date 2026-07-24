package com.gsserver.ui.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsserver.security")
public class SecurityUsersProperties {

  private final List<UserEntry> users = new ArrayList<>();

  /** Path to the JSON file that persists runtime-managed users (seeded from {@link #users}). */
  private String usersFile = "/WORKSPACE/Settings/GS-Server/users.json";

  /**
   * Master switch for passwordless {@code thor} superadmin login. Even when {@code true}, thor is
   * only accepted for genuine localhost requests (see {@code RequestOriginUtils}); proxied public
   * requests are always rejected. Set {@code false} to disable thor entirely.
   */
  private boolean thorLoginEnabled = true;

  public List<UserEntry> getUsers() {
    return users;
  }

  public String getUsersFile() {
    return usersFile;
  }

  public void setUsersFile(String usersFile) {
    this.usersFile = usersFile;
  }

  public boolean isThorLoginEnabled() {
    return thorLoginEnabled;
  }

  public void setThorLoginEnabled(boolean thorLoginEnabled) {
    this.thorLoginEnabled = thorLoginEnabled;
  }

  public static class UserEntry {
    private String username;
    private String password;
    private List<String> authorities = new ArrayList<>();

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public List<String> getAuthorities() {
      return authorities;
    }

    public void setAuthorities(List<String> authorities) {
      this.authorities = authorities;
    }
  }
}
