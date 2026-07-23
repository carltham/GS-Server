package com.gsserver.ui.hardening.adapter;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LinuxHardeningAdapter {
  private final HardeningCommandExecutor commandExecutor;

  public LinuxHardeningAdapter(HardeningCommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public HardeningExecutionReport applyBaselineHardening() {
    List<String> command =
        List.of(
            "/usr/bin/env",
            "bash",
            "-lc",
            "echo linux-hardening && echo no-error 1>&2 && exit 0");

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "linux", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }
}
