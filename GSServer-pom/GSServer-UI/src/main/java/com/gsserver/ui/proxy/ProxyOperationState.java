package com.gsserver.ui.proxy;

public record ProxyOperationState(
    String operationId,
    String occurredAtUtc,
    String status,
    String tenantId,
    String requestedBy,
    boolean enabled,
    String upstreamHost,
    int upstreamPort,
    boolean tlsEnabled,
    String message) {}
