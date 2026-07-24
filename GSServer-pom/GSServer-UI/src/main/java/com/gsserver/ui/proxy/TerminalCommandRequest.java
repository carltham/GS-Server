package com.gsserver.ui.proxy;

public record TerminalCommandRequest(String command, String sudoUser) {
  public TerminalCommandRequest(String command) {
    this(command, null);
  }
}
