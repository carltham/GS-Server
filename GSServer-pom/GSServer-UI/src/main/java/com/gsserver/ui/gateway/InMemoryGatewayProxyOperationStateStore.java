package com.gsserver.ui.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGatewayProxyOperationStateStore implements GatewayProxyOperationStateStore {
  private final Map<String, GatewayProxyOperationState> states = new LinkedHashMap<>();

  @Override
  public void save(GatewayProxyOperationState state) {
    states.put(state.operationId(), state);
  }

  @Override
  public Optional<GatewayProxyOperationState> getLatest() {
    return states.values().stream().reduce((first, second) -> second);
  }

  @Override
  public Optional<GatewayProxyOperationState> getById(String operationId) {
    return Optional.ofNullable(states.get(operationId));
  }
}
