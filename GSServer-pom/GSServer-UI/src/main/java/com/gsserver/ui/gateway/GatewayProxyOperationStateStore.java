package com.gsserver.ui.gateway;

import java.util.Optional;

public interface GatewayProxyOperationStateStore {
  void save(GatewayProxyOperationState state);

  Optional<GatewayProxyOperationState> getLatest();

  Optional<GatewayProxyOperationState> getById(String operationId);
}
