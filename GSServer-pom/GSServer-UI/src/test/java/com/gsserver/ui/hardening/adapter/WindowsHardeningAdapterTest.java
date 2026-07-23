package com.gsserver.ui.hardening.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WindowsHardeningAdapterTest {

  @Test
  void applyBaselineHardeningCapturesStdoutStderrAndExitCode() {
    List<String> executedCommand = new ArrayList<>();
    Duration[] executedTimeout = new Duration[1];

    HardeningCommandExecutor executor =
        (command, timeout) -> {
          executedCommand.addAll(command);
          executedTimeout[0] = timeout;
          return new CommandExecutionResult(0, "windows ok", "warn", false);
        };

    WindowsHardeningAdapter adapter = new WindowsHardeningAdapter(executor);
    HardeningExecutionReport report = adapter.applyBaselineHardening();

    assertThat(executedCommand).isNotEmpty();
    assertThat(executedCommand).contains("powershell");
    assertThat(executedTimeout[0]).isEqualTo(Duration.ofMinutes(2));
    assertThat(report.platform()).isEqualTo("windows");
    assertThat(report.stdout()).isEqualTo("windows ok");
    assertThat(report.stderr()).isEqualTo("warn");
    assertThat(report.exitCode()).isEqualTo(0);
    assertThat(report.timedOut()).isFalse();
    assertThat(report.successful()).isTrue();
  }

  @Test
  void applyBaselineHardeningMarksFailureOnTimeout() {
    HardeningCommandExecutor executor =
        (command, timeout) -> new CommandExecutionResult(0, "", "timeout", true);

    WindowsHardeningAdapter adapter = new WindowsHardeningAdapter(executor);
    HardeningExecutionReport report = adapter.applyBaselineHardening();

    assertThat(report.timedOut()).isTrue();
    assertThat(report.successful()).isFalse();
  }
}
