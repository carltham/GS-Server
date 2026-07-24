package com.gsserver.ui.hardening;

import com.gsserver.ui.db.FileBasedHardeningOperationStateRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class InMemoryHardeningOperationStateStore implements HardeningOperationStateStore {
  private final AtomicReference<HardeningOperationState> latest = new AtomicReference<>();
  private final FileBasedHardeningOperationStateRepository repository;

  public InMemoryHardeningOperationStateStore(FileBasedHardeningOperationStateRepository repository) {
    this.repository = repository;
    this.latest.set(repository.getLatest().orElse(null));
  }

  public InMemoryHardeningOperationStateStore() {
    this.repository = null;
  }

  @Override
  public void save(HardeningOperationState state) {
    latest.set(state);
    if (repository != null) {
      repository.save(state);
    }
  }

  @Override
  public Optional<HardeningOperationState> getLatest() {
    return Optional.ofNullable(latest.get());
  }
}
