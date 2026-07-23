package com.gsserver.ui.hardening.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessHardeningCommandExecutorTest {

  @Test
  void executeCapturesStdoutStderrAndExitCode() {
    ProcessHardeningCommandExecutor executor = new ProcessHardeningCommandExecutor();

    CommandExecutionResult result =
        executor.execute(
            List.of("/usr/bin/env", "bash", "-lc", "echo hello && echo warn 1>&2 && exit 0"),
            Duration.ofSeconds(2));

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).contains("hello");
    assertThat(result.stderr()).contains("warn");
    assertThat(result.timedOut()).isFalse();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void executeCapturesFailureExitCode() {
    ProcessHardeningCommandExecutor executor = new ProcessHardeningCommandExecutor();

    CommandExecutionResult result =
        executor.execute(List.of("/usr/bin/env", "bash", "-lc", "echo bad && exit 7"), Duration.ofSeconds(2));

    assertThat(result.exitCode()).isEqualTo(7);
    assertThat(result.stdout()).contains("bad");
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  void executeMarksTimeout() {
    ProcessHardeningCommandExecutor executor = new ProcessHardeningCommandExecutor();

    CommandExecutionResult result =
        executor.execute(
            List.of("/usr/bin/env", "bash", "-lc", "tail -f /dev/null"), Duration.ofMillis(200));

    assertThat(result.timedOut()).isTrue();
    assertThat(result.exitCode()).isEqualTo(-1);
    assertThat(result.isSuccess()).isFalse();
  }
}
