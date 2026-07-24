package com.gsserver.ui.gateway;

import com.gsserver.ui.db.FileBasedGatewayProxyOperationStateRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGatewayProxyOperationStateStore implements GatewayProxyOperationStateStore {
  private final Map<String, GatewayProxyOperationState> states = new LinkedHashMap<>();
  private final FileBasedGatewayProxyOperationStateRepository repository;

  public InMemoryGatewayProxyOperationStateStore(FileBasedGatewayProxyOperationStateRepository repository) {
    this.repository = repository;
  }

  public InMemoryGatewayProxyOperationStateStore() {
    this.repository = null;
  }

  @Override
  public void save(GatewayProxyOperationState state) {
    states.put(state.operationId(), state);
    if (repository != null) {
      repository.save(state);
    }
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
