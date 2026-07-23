package com.gsserver.ui.proxy;

import java.util.Optional;

public interface ProxyOperationStateStore {
  void save(ProxyOperationState state);

  Optional<ProxyOperationState> getLatest();
}
