package com.gsserver.ui.gateway;

public record GatewayProxyOperationState(
    String operationId,
    String occurredAtUtc,
    String status,
    String tenantId,
    String requestedBy,
    String upstreamHost,
    int upstreamPort,
    boolean tlsEnabled,
    String tlsCertPath,
    String tlsKeyPath,
    String previousStateId,
    String message) {}
