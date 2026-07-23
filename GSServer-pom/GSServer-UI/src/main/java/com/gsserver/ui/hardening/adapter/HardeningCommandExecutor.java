package com.gsserver.ui.hardening.adapter;

import java.time.Duration;
import java.util.List;

public interface HardeningCommandExecutor {
  CommandExecutionResult execute(List<String> command, Duration timeout);
}
