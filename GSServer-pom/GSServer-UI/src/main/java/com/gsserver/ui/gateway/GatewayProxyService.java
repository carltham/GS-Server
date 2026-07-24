package com.gsserver.ui.gateway;

import java.util.Optional;

public interface GatewayProxyService {
  GatewayProxyResponse configureNginxProxy(GatewayProxyRequest request);

  Optional<GatewayProxyOperationState> getLatestOperationState();

  GatewayProxyResponse rollbackToState(String operationId);
}
