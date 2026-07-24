package com.gsserver.ui.proxy;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Captures the actual HTTP port the application is listening on, so the generated reverse-proxy
 * template can point nginx at this very app.
 */
@Component
public class ServerPortHolder implements ApplicationListener<WebServerInitializedEvent> {
  private volatile int port = 8080;

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    this.port = event.getWebServer().getPort();
  }

  public int getPort() {
    return port;
  }
}
