package com.gsserver.ui.terminal;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single interactive terminal session backed by a real pseudo-terminal (PTY).
 *
 * <p>The PTY is what makes the terminal "truly interactive": the child shell (and anything it
 * launches, such as {@code sudo}) sees a genuine TTY, so password prompts, coloured output, line
 * editing and full-screen programs all behave exactly as they would in a native terminal. The
 * shell runs as the JVM's own OS user (e.g. {@code carl}); the user simply types {@code sudo ...}
 * and answers the prompt live.
 */
public class InteractiveTerminalSession {
  private static final Logger logger = LoggerFactory.getLogger(InteractiveTerminalSession.class);
  private static final int READ_BUFFER_SIZE = 8192;

  private final String sessionId;
  private final PtyProcess process;
  private final OutputStream ptyInput;
  private final Thread readerThread;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  /**
   * Start an interactive login shell attached to a fresh PTY.
   *
   * @param sessionId unique session identifier
   * @param cols initial terminal width in columns
   * @param rows initial terminal height in rows
   * @param runAsUser OS user the shell should run as; when blank or equal to the server user a plain
   *     login shell is started, otherwise the shell is launched via {@code sudo -u <user> -i}
   * @param onOutput receives raw bytes produced by the PTY (stdout+stderr are merged by the TTY)
   * @param onExit receives the shell exit code once the process ends
   * @throws IOException if the PTY process cannot be started
   */
  public InteractiveTerminalSession(
      String sessionId,
      int cols,
      int rows,
      String runAsUser,
      Consumer<byte[]> onOutput,
      IntConsumer onExit)
      throws IOException {
    this.sessionId = sessionId;

    Map<String, String> env = new HashMap<>(System.getenv());
    env.put("TERM", "xterm-256color");
    // Ensure the shell knows its size before the first prompt is drawn.
    env.put("COLUMNS", Integer.toString(sanitizeDimension(cols, 80)));
    env.put("LINES", Integer.toString(sanitizeDimension(rows, 24)));

    String home = System.getProperty("user.home", "/");
    String[] command = buildShellCommand(runAsUser);

    PtyProcessBuilder builder =
        new PtyProcessBuilder()
            .setCommand(command)
            .setEnvironment(env)
            .setDirectory(home)
            .setInitialColumns(sanitizeDimension(cols, 80))
            .setInitialRows(sanitizeDimension(rows, 24))
            .setConsole(false)
            .setRedirectErrorStream(true);

    this.process = builder.start();
    this.ptyInput = process.getOutputStream();

    this.readerThread = startReader(process.getInputStream(), onOutput);
    startMonitor(onExit);

    logger.info(
        "Terminal session {} started ({}x{}) running as {}",
        sessionId,
        cols,
        rows,
        effectiveUser(runAsUser));
  }

  /**
   * Build the shell command. A plain login shell runs directly as the server user; any other user is
   * reached with {@code sudo -u <user> -i}, whose password prompt appears live on the PTY.
   */
  private static String[] buildShellCommand(String runAsUser) {
    String currentUser = System.getProperty("user.name", "");
    if (runAsUser == null || runAsUser.isBlank() || runAsUser.equals(currentUser)) {
      return new String[] {"/bin/bash", "-il"};
    }
    return new String[] {"sudo", "-u", runAsUser, "-i"};
  }

  private static String effectiveUser(String runAsUser) {
    String currentUser = System.getProperty("user.name", "unknown");
    return (runAsUser == null || runAsUser.isBlank()) ? currentUser : runAsUser;
  }

  private Thread startReader(InputStream ptyOutput, Consumer<byte[]> onOutput) {
    Thread thread =
        new Thread(
            () -> {
              byte[] buffer = new byte[READ_BUFFER_SIZE];
              try {
                int read;
                while ((read = ptyOutput.read(buffer)) != -1) {
                  byte[] chunk = new byte[read];
                  System.arraycopy(buffer, 0, chunk, 0, read);
                  onOutput.accept(chunk);
                }
              } catch (IOException e) {
                if (!terminated.get()) {
                  logger.debug("PTY read ended for session {}: {}", sessionId, e.getMessage());
                }
              }
            },
            "pty-reader-" + sessionId);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private void startMonitor(IntConsumer onExit) {
    Thread monitor =
        new Thread(
            () -> {
              try {
                int code = process.waitFor();
                logger.info("Terminal session {} exited with code {}", sessionId, code);
                onExit.accept(code);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            "pty-monitor-" + sessionId);
    monitor.setDaemon(true);
    monitor.start();
  }

  /** Write raw keystroke bytes to the PTY. */
  public void write(byte[] data) throws IOException {
    if (terminated.get()) {
      throw new IOException("Session is not active");
    }
    ptyInput.write(data);
    ptyInput.flush();
  }

  /** Apply a new terminal window size (TIOCSWINSZ) so full-screen programs redraw correctly. */
  public void resize(int cols, int rows) {
    if (terminated.get()) {
      return;
    }
    try {
      process.setWinSize(new WinSize(sanitizeDimension(cols, 80), sanitizeDimension(rows, 24)));
    } catch (Exception e) {
      logger.debug("Resize failed for session {}: {}", sessionId, e.getMessage());
    }
  }

  /** Destroy the shell process and release the PTY. */
  public void terminate() {
    if (!terminated.compareAndSet(false, true)) {
      return;
    }
    try {
      process.destroy();
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      try {
        ptyInput.close();
      } catch (IOException ignored) {
        // best-effort
      }
      readerThread.interrupt();
    }
    logger.info("Terminal session {} terminated", sessionId);
  }

  public boolean isActive() {
    return !terminated.get() && process.isAlive();
  }

  public String getSessionId() {
    return sessionId;
  }

  private static int sanitizeDimension(int value, int fallback) {
    return value > 0 && value <= 1000 ? value : fallback;
  }
}
