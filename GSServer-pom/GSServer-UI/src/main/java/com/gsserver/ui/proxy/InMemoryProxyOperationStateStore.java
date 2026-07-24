package com.gsserver.ui.proxy;

import com.gsserver.ui.db.FileBasedProxyOperationStateRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class InMemoryProxyOperationStateStore implements ProxyOperationStateStore {
  private final AtomicReference<ProxyOperationState> latest = new AtomicReference<>();
  private final FileBasedProxyOperationStateRepository repository;

  public InMemoryProxyOperationStateStore(FileBasedProxyOperationStateRepository repository) {
    this.repository = repository;
    this.latest.set(repository.getLatest().orElse(null));
  }

  public InMemoryProxyOperationStateStore() {
    this.repository = null;
  }

  @Override
  public void save(ProxyOperationState state) {
    latest.set(state);
    if (repository != null) {
      repository.save(state);
    }
  }

  @Override
  public Optional<ProxyOperationState> getLatest() {
    return Optional.ofNullable(latest.get());
  }
}
