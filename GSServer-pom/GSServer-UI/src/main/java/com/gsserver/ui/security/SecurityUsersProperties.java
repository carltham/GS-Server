package com.gsserver.ui.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsserver.security")
public class SecurityUsersProperties {

  private final List<UserEntry> users = new ArrayList<>();

  public List<UserEntry> getUsers() {
    return users;
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
