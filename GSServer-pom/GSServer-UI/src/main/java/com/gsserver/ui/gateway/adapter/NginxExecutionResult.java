package com.gsserver.ui.gateway.adapter;

public record NginxExecutionResult(
    boolean success,
    String message,
    String previousConfigBackupPath) {}
