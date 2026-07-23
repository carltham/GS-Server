package com.gsserver.ui.hardening;

import org.springframework.stereotype.Service;

@Service
public class DefaultHardeningService implements HardeningService {

  @Override
  public HardeningResponse triggerHardening(HardeningRequest request) {
    return new HardeningResponse("accepted", "Hardening request accepted");
  }
}
