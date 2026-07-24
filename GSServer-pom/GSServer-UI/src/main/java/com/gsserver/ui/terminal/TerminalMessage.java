package com.gsserver.ui.terminal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * WebSocket messages exchanged with an interactive PTY terminal session.
 *
 * <p>Terminal payloads ({@link Input}, {@link Output}) carry raw PTY bytes as Base64 so that
 * arbitrary control sequences and partial multi-byte UTF-8 characters survive transport intact.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = TerminalMessage.Input.class, name = "INPUT"),
  @JsonSubTypes.Type(value = TerminalMessage.Resize.class, name = "RESIZE"),
  @JsonSubTypes.Type(value = TerminalMessage.Output.class, name = "OUTPUT"),
  @JsonSubTypes.Type(value = TerminalMessage.Exit.class, name = "EXIT"),
  @JsonSubTypes.Type(value = TerminalMessage.ErrorMessage.class, name = "ERROR"),
})
public abstract class TerminalMessage {
  @JsonProperty("type")
  public abstract String getType();

  /** Client -> server: raw keystroke bytes (Base64) to write to the PTY. */
  public static final class Input extends TerminalMessage {
    @JsonProperty("data")
    public String data;

    public Input() {}

    @Override
    public String getType() {
      return "INPUT";
    }
  }

  /** Client -> server: terminal window size change (TIOCSWINSZ). */
  public static final class Resize extends TerminalMessage {
    @JsonProperty("cols")
    public int cols;

    @JsonProperty("rows")
    public int rows;

    public Resize() {}

    @Override
    public String getType() {
      return "RESIZE";
    }
  }

  /** Server -> client: raw PTY output bytes (Base64). */
  public static final class Output extends TerminalMessage {
    @JsonProperty("data")
    public String data;

    public Output() {}

    public Output(String data) {
      this.data = data;
    }

    @Override
    public String getType() {
      return "OUTPUT";
    }
  }

  /** Server -> client: the shell process exited. */
  public static final class Exit extends TerminalMessage {
    @JsonProperty("exitCode")
    public int exitCode;

    public Exit() {}

    public Exit(int exitCode) {
      this.exitCode = exitCode;
    }

    @Override
    public String getType() {
      return "EXIT";
    }
  }

  /** Server -> client: an error occurred handling the session. */
  public static final class ErrorMessage extends TerminalMessage {
    @JsonProperty("message")
    public String message;

    public ErrorMessage() {}

    public ErrorMessage(String message) {
      this.message = message;
    }

    @Override
    public String getType() {
      return "ERROR";
    }
  }
}
