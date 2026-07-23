package com.gsserver.ui.proxy;

import java.util.List;

public record ProxyInstallGuideResponse(String operatingSystem, List<String> steps, List<String> commands) {}
