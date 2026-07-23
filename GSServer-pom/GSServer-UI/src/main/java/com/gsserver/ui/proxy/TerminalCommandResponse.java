package com.gsserver.ui.proxy;

public record TerminalCommandResponse(
    String command, int exitCode, String stdout, String stderr, long durationMs) {}
