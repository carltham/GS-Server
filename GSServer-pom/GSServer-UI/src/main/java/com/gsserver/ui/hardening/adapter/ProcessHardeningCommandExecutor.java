package com.gsserver.ui.hardening.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProcessHardeningCommandExecutor implements HardeningCommandExecutor {

  @Override
  public CommandExecutionResult execute(List<String> command, Duration timeout) {
    try {
      Process process = new ProcessBuilder(command).start();

      CompletableFuture<String> stdoutFuture =
          CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
      CompletableFuture<String> stderrFuture =
          CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));

      boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        process.waitFor(1, TimeUnit.SECONDS);
      }

      int exitCode = finished ? process.exitValue() : -1;
      String stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
      String stderr = stderrFuture.get(2, TimeUnit.SECONDS);

      return new CommandExecutionResult(exitCode, stdout, stderr, !finished);
    } catch (Exception exception) {
      return new CommandExecutionResult(
          -1, "", "Command execution error: " + exception.getMessage(), false);
    }
  }

  private String readAll(InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      return "";
    }
  }
}
