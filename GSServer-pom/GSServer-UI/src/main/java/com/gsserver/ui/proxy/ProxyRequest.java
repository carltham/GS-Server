package com.gsserver.ui.proxy;

public record ProxyRequest(
    String tenantId,
    String requestedBy,
    boolean enabled,
    String upstreamHost,
    int upstreamPort,
    boolean tlsEnabled) {}
