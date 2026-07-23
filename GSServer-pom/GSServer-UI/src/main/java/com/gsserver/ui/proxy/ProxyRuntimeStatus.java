package com.gsserver.ui.proxy;

public record ProxyRuntimeStatus(boolean nginxRunning, boolean apacheRunning, String detectedServer) {}
