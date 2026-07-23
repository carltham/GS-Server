package com.gsserver.ui.hardening;

import java.util.Optional;

public interface HardeningService {
  HardeningResponse triggerHardening(HardeningRequest request);

  Optional<HardeningOperationState> getLatestOperationState();
}
