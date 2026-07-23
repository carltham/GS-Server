package com.gsserver.ui.hardening.adapter;

public record CommandExecutionResult(int exitCode, String stdout, String stderr, boolean timedOut) {

  public boolean isSuccess() {
    return exitCode == 0 && !timedOut;
  }
}
