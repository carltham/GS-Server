package com.gsserver.ui.gateway.adapter;

public record NginxConfigurationCommand(
    String upstreamHost,
    int upstreamPort,
    boolean tlsEnabled,
    String tlsCertPath,
    String tlsKeyPath) {}
