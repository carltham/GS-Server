package com.gsserver.ui.admin;

import java.util.List;

/** Request body to update a user's roles and enabled state. */
public record UpdateUserRequest(List<String> authorities, Boolean enabled) {}
