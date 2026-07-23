package com.gsserver.ui.proxy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class InMemoryProxyOperationStateStore implements ProxyOperationStateStore {
  private final AtomicReference<ProxyOperationState> latest = new AtomicReference<>();

  @Override
  public void save(ProxyOperationState state) {
    latest.set(state);
  }

  @Override
  public Optional<ProxyOperationState> getLatest() {
    return Optional.ofNullable(latest.get());
  }
}
