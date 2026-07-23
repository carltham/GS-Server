package com.gsserver.ui.hardening.adapter;

public record HardeningExecutionReport(
    String platform, int exitCode, String stdout, String stderr, boolean timedOut) {

  public boolean successful() {
    return exitCode == 0 && !timedOut;
  }
}
