package com.gsserver.ui.proxy;

import com.gsserver.ui.hardening.PolicyViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class ProxyInstallationService {
  private static final int COMMAND_TIMEOUT_SECONDS = 90;
  private static final int MAX_OUTPUT_CHARS = 16_000;

  private static final Set<String> ALLOWED_EXECUTABLES =
      Set.of(
          "apt",
          "apt-get",
          "dnf",
          "yum",
          "pacman",
          "zypper",
          "systemctl",
          "service",
          "nginx",
          "apache2ctl",
          "httpd",
          "which",
          "cat",
          "ls",
          "lsb_release",
          "ps");

  public ProxyInstallGuideResponse getInstallGuide() {
    String osName = System.getProperty("os.name", "unknown");
    String osLower = osName.toLowerCase(Locale.ROOT);

    if (osLower.contains("linux")) {
      return new ProxyInstallGuideResponse(
          "Linux",
          List.of(
              "Install NGINX or Apache using your distro package manager.",
              "Enable the service at boot and start it now.",
              "Check service status and confirm the binary path.",
              "Return to Proxy Management and re-run detection."),
          linuxCommands());
    }

    if (osLower.contains("win")) {
      return new ProxyInstallGuideResponse(
          "Windows",
          List.of(
              "Install NGINX or Apache using the official installer or package manager.",
              "Run the service as Administrator.",
              "Verify the process is running before returning to Proxy Management."),
          List.of("echo Windows install is not automated in this terminal runner."));
    }

    if (osLower.contains("mac")) {
      return new ProxyInstallGuideResponse(
          "macOS",
          List.of(
              "Install NGINX or Apache using Homebrew.",
              "Start the service and verify process status.",
              "Return to Proxy Management and re-run detection."),
          List.of("echo macOS install is not automated in this terminal runner."));
    }

    return new ProxyInstallGuideResponse(
        osName,
        List.of("Unsupported operating system for guided proxy installation."),
        List.of());
  }

  public TerminalCommandResponse executeCommand(TerminalCommandRequest request) {
    if (request == null || isBlank(request.command())) {
      throw new PolicyViolationException("Command is required.");
    }

    String command = request.command().trim();
    validateCommand(command);

    List<String> tokens = tokenize(command);
    Instant startedAt = Instant.now();

    Process process;
    try {
      process = new ProcessBuilder(tokens).start();
    } catch (IOException exception) {
      throw new PolicyViolationException("Failed to start command: " + exception.getMessage());
    }

    boolean finished;
    try {
      finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PolicyViolationException("Command execution interrupted.");
    }

    if (!finished) {
      process.destroyForcibly();
      long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
      return new TerminalCommandResponse(command, 124, "", "Command timed out.", durationMs);
    }

    int exitCode = process.exitValue();
    long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
    String stdout = readStream(process.getInputStream());
    String stderr = readStream(process.getErrorStream());

    return new TerminalCommandResponse(command, exitCode, limitOutput(stdout), limitOutput(stderr), durationMs);
  }

  private List<String> linuxCommands() {
    List<String> commands = new ArrayList<>();
    commands.add("cat /etc/os-release");
    commands.add("which apt");
    commands.add("sudo apt update");
    commands.add("sudo apt install -y nginx");
    commands.add("sudo systemctl enable nginx");
    commands.add("sudo systemctl start nginx");
    commands.add("sudo systemctl status nginx");
    commands.add("ps aux");
    return commands;
  }

  private void validateCommand(String command) {
    if (command.length() > 200) {
      throw new PolicyViolationException("Command length exceeds allowed limit.");
    }

    if (command.contains("&&")
        || command.contains("||")
        || command.contains(";")
        || command.contains("|")
        || command.contains("`")
        || command.contains(">")
        || command.contains("<")
        || command.contains("$")
        || command.contains("\n")) {
      throw new PolicyViolationException("Command contains unsupported shell operators.");
    }

    List<String> tokens = tokenize(command);
    if (tokens.isEmpty()) {
      throw new PolicyViolationException("Command is required.");
    }

    int executableIndex = 0;
    if ("sudo".equals(tokens.get(0))) {
      executableIndex = 1;
    }

    if (executableIndex >= tokens.size()) {
      throw new PolicyViolationException("Command executable is missing.");
    }

    String executable = tokens.get(executableIndex);
    if (!ALLOWED_EXECUTABLES.contains(executable)) {
      throw new PolicyViolationException("Command is not allowed in installation terminal.");
    }
  }

  private List<String> tokenize(String command) {
    String[] rawTokens = command.split("\\s+");
    List<String> tokens = new ArrayList<>();
    for (String token : rawTokens) {
      if (!token.isBlank()) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  private String readStream(InputStream stream) {
    try {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      return "Failed to read process output: " + exception.getMessage();
    }
  }

  private String limitOutput(String output) {
    if (output == null || output.length() <= MAX_OUTPUT_CHARS) {
      return output == null ? "" : output;
    }
    return output.substring(0, MAX_OUTPUT_CHARS) + "\n[output truncated]";
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
