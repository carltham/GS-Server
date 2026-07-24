package com.gsserver.ui.admin;

import java.util.List;

/** A user as exposed to the management UI — never includes the password hash. */
public record UserSummary(String username, List<String> authorities, boolean enabled) {}
