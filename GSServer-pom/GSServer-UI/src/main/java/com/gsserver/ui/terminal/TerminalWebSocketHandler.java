package com.gsserver.ui.terminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Bridges a browser WebSocket to a PTY-backed shell.
 *
 * <p>A fresh interactive shell is spawned as soon as the socket opens. Keystrokes arrive as
 * {@code INPUT} messages and are written verbatim to the PTY; PTY output is streamed back as
 * {@code OUTPUT} messages. Both payloads are Base64 so raw control bytes survive transport.
 */
public class TerminalWebSocketHandler extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
  private static final int SEND_BUFFER_LIMIT = 512 * 1024;
  private static final int SEND_TIME_LIMIT_MS = 10_000;
  // Conservative POSIX-ish username: starts with a letter/underscore, then letters/digits/_/-.
  private static final Pattern VALID_USERNAME = Pattern.compile("^[a-z_][a-z0-9_-]{0,31}$");

  private final TerminalSessionManager sessionManager;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Per-connection state, keyed by WebSocket session id. */
  private final Map<String, Connection> connections = new ConcurrentHashMap<>();

  public TerminalWebSocketHandler(TerminalSessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // sendMessage() is called from the PTY reader/monitor threads as well as this one, so wrap the
    // session in a decorator that serialises concurrent sends.
    WebSocketSession safe =
        new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);

    String runAsUser;
    try {
      runAsUser = resolveRunAsUser(session.getUri());
      ensureUserLaunchable(runAsUser);
    } catch (IllegalArgumentException e) {
      sendError(safe, e.getMessage());
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }

    try {
      InteractiveTerminalSession terminal =
          sessionManager.createSession(
              80,
              24,
              runAsUser,
              bytes -> sendOutput(safe, bytes),
              exitCode -> handleExit(safe, exitCode));
      connections.put(session.getId(), new Connection(safe, terminal));
      logger.info("Terminal WebSocket opened: {} -> {}", session.getId(), terminal.getSessionId());
    } catch (IOException e) {
      logger.error("Failed to start terminal for {}: {}", session.getId(), e.getMessage());
      sendError(safe, "Failed to start terminal: " + e.getMessage());
      session.close(CloseStatus.SERVER_ERROR);
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Connection connection = connections.get(session.getId());
    if (connection == null) {
      return;
    }

    TerminalMessage parsed;
    try {
      parsed = objectMapper.readValue(message.getPayload(), TerminalMessage.class);
    } catch (Exception e) {
      sendError(connection.session, "Malformed message: " + e.getMessage());
      return;
    }

    if (parsed instanceof TerminalMessage.Input input) {
      try {
        connection.terminal.write(Base64.getDecoder().decode(input.data));
      } catch (IOException | IllegalArgumentException e) {
        sendError(connection.session, "Failed to send input: " + e.getMessage());
      }
    } else if (parsed instanceof TerminalMessage.Resize resize) {
      connection.terminal.resize(resize.cols, resize.rows);
    } else {
      sendError(connection.session, "Unsupported message type: " + parsed.getType());
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    logger.warn("Terminal WebSocket transport error {}: {}", session.getId(), exception.getMessage());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Connection connection = connections.remove(session.getId());
    if (connection != null) {
      connection.terminal.terminate();
    }
    logger.info("Terminal WebSocket closed: {} - {}", session.getId(), status);
  }

  /**
   * Read and validate the optional {@code user} query parameter naming the OS user the shell should
   * run as. Returns an empty string when absent (meaning: run as the server user).
   *
   * @throws IllegalArgumentException if the supplied username is not a valid POSIX username
   */
  private String resolveRunAsUser(URI uri) {
    if (uri == null || uri.getQuery() == null) {
      return "";
    }
    for (String pair : uri.getQuery().split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0 && "user".equals(pair.substring(0, eq))) {
        String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8).trim();
        if (value.isEmpty()) {
          return "";
        }
        if (!VALID_USERNAME.matcher(value).matches()) {
          throw new IllegalArgumentException("Invalid username: " + value);
        }
        return value;
      }
    }
    return "";
  }

  /**
   * Reject the connection up-front if the requested user cannot be launched — either it is not a
   * valid username or no such OS account exists. Blank (server user) is always allowed.
   *
   * @throws IllegalArgumentException if the user does not exist on this host
   */
  private void ensureUserLaunchable(String runAsUser) {
    if (runAsUser.isBlank() || runAsUser.equals(System.getProperty("user.name", ""))) {
      return;
    }
    if (!userExists(runAsUser)) {
      throw new IllegalArgumentException("Unknown user: " + runAsUser);
    }
  }

  private boolean userExists(String user) {
    try {
      Process process =
          new ProcessBuilder("id", "-u", user).redirectErrorStream(true).start();
      process.getInputStream().readAllBytes(); // drain so the process can exit
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void sendOutput(WebSocketSession session, byte[] bytes) {
    String encoded = Base64.getEncoder().encodeToString(bytes);
    send(session, new TerminalMessage.Output(encoded));
  }

  private void handleExit(WebSocketSession session, int exitCode) {
    send(session, new TerminalMessage.Exit(exitCode));
    try {
      session.close(CloseStatus.NORMAL);
    } catch (IOException ignored) {
      // already closing
    }
  }

  private void sendError(WebSocketSession session, String message) {
    send(session, new TerminalMessage.ErrorMessage(message));
  }

  private void send(WebSocketSession session, TerminalMessage message) {
    if (!session.isOpen()) {
      return;
    }
    try {
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    } catch (IOException e) {
      logger.debug("Failed to send {} to {}: {}", message.getType(), session.getId(), e.getMessage());
    }
  }

  /** Pairs a (thread-safe) WebSocket session with its PTY terminal. */
  private record Connection(WebSocketSession session, InteractiveTerminalSession terminal) {}
}
