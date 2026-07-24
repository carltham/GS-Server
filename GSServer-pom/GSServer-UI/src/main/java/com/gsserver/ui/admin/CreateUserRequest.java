package com.gsserver.ui.admin;

import java.util.List;

/** Request body to create a user. {@code enabled} defaults to true when null. */
public record CreateUserRequest(
    String username, String password, List<String> authorities, Boolean enabled) {}
