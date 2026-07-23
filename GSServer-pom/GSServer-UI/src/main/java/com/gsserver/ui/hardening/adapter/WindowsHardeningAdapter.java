package com.gsserver.ui.hardening.adapter;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WindowsHardeningAdapter {
  private final HardeningCommandExecutor commandExecutor;

  public WindowsHardeningAdapter(HardeningCommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public HardeningExecutionReport applyBaselineHardening() {
    List<String> command =
        List.of(
            "powershell",
            "-NoProfile",
            "-Command",
            "Write-Output 'windows-hardening'; Write-Error 'no-error'; exit 0");

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "windows", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }
}
