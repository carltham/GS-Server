package com.gsserver.ui.terminal;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tracks active interactive PTY terminal sessions and their lifecycle.
 */
@Service
public class TerminalSessionManager {
  private static final Logger logger = LoggerFactory.getLogger(TerminalSessionManager.class);

  private final ConcurrentHashMap<String, InteractiveTerminalSession> sessions =
      new ConcurrentHashMap<>();

  /**
   * Create and start a new PTY-backed shell session.
   *
   * @param cols initial terminal width
   * @param rows initial terminal height
   * @param runAsUser OS user the shell should run as (blank for the server user)
   * @param onOutput receives raw PTY output bytes
   * @param onExit receives the shell exit code
   * @return the started session
   * @throws IOException if the PTY process cannot be started
   */
  public InteractiveTerminalSession createSession(
      int cols, int rows, String runAsUser, Consumer<byte[]> onOutput, IntConsumer onExit)
      throws IOException {
    String sessionId = UUID.randomUUID().toString();
    InteractiveTerminalSession session =
        new InteractiveTerminalSession(sessionId, cols, rows, runAsUser, onOutput, onExit);
    sessions.put(sessionId, session);
    logger.info("Created terminal session {} (active: {})", sessionId, sessions.size());
    return session;
  }

  public void terminateSession(String sessionId) {
    InteractiveTerminalSession session = sessions.remove(sessionId);
    if (session != null) {
      session.terminate();
    }
  }

  public int getActiveSessionCount() {
    return sessions.size();
  }

  @PreDestroy
  public void shutdown() {
    sessions.values().forEach(InteractiveTerminalSession::terminate);
    sessions.clear();
    logger.info("Terminal session manager shut down");
  }
}
