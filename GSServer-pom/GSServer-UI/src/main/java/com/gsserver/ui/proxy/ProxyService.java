package com.gsserver.ui.proxy;

import java.util.Optional;

public interface ProxyService {
  ProxyResponse applyProxyConfig(ProxyRequest request);

  Optional<ProxyOperationState> getLatestOperationState();

  ProxyRuntimeStatus getRuntimeStatus();
}
