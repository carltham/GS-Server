package com.gsserver.ui.hardening;

public record HardeningOperationState(
    String operationId,
    String occurredAtUtc,
    String status,
    String tenantId,
    String requestedBy,
    String profile,
    String platform,
    int exitCode,
    boolean timedOut,
    String rollbackStatus,
    String message) {}
