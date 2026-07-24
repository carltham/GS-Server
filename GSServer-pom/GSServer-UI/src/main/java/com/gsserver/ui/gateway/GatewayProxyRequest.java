package com.gsserver.ui.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GatewayProxyRequest(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("requestedBy") String requestedBy,
    @JsonProperty("upstreamHost") String upstreamHost,
    @JsonProperty("upstreamPort") int upstreamPort,
    @JsonProperty("tlsEnabled") boolean tlsEnabled,
    @JsonProperty("tlsCertPath") String tlsCertPath,
    @JsonProperty("tlsKeyPath") String tlsKeyPath) {}
