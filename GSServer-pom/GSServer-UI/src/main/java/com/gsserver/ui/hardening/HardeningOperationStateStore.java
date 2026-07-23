package com.gsserver.ui.hardening;

import java.util.Optional;

public interface HardeningOperationStateStore {
  void save(HardeningOperationState state);

  Optional<HardeningOperationState> getLatest();
}
