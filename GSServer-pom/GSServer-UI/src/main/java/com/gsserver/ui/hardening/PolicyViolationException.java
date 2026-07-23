package com.gsserver.ui.hardening;

public class PolicyViolationException extends RuntimeException {
  public PolicyViolationException(String message) {
    super(message);
  }
}
