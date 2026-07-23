package com.gsserver.ui.hardening;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class InMemoryHardeningOperationStateStore implements HardeningOperationStateStore {
  private final AtomicReference<HardeningOperationState> latest = new AtomicReference<>();

  @Override
  public void save(HardeningOperationState state) {
    latest.set(state);
  }

  @Override
  public Optional<HardeningOperationState> getLatest() {
    return Optional.ofNullable(latest.get());
  }
}
