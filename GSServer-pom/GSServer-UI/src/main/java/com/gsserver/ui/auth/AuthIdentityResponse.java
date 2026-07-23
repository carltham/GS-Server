package com.gsserver.ui.auth;

import java.util.List;

public record AuthIdentityResponse(String username, List<String> authorities) {}
