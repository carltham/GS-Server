package com.gsserver.ui.gateway;

public class GatewayProxyExecutionException extends RuntimeException {
  public GatewayProxyExecutionException(String message) {
    super(message);
  }

  public GatewayProxyExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
