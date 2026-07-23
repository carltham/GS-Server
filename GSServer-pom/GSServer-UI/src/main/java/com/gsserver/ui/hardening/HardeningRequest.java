package com.gsserver.ui.hardening;

public record HardeningRequest(String tenantId, String requestedBy, String profile) {}
