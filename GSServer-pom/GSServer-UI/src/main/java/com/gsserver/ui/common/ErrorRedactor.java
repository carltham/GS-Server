package com.gsserver.ui.common;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/**
 * Redacts secrets from error messages, logs, and stack traces.
 *
 * Prevents credentials, API keys, file paths, and other sensitive data
 * from leaking in error responses sent to clients.
 */
@Component
public class ErrorRedactor {

  // Regex patterns for common secrets
  private static final Pattern PASSWORD_IN_URL = Pattern.compile("([^:]+):([^@]+)@");
  private static final Pattern JDBC_CONNECTION = Pattern.compile("jdbc:[^\\s]+");
  private static final Pattern SSH_KEY_PATH = Pattern.compile("/[^\\s]*\\.ssh[^\\s]*");
  private static final Pattern API_KEY = Pattern.compile("(api[_-]?key|api[_-]?token|bearer\\s+)[\\w]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern PASSWORD_LITERAL = Pattern.compile("(password|passwd|pwd)[\\s]*=\\s*[\\w]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTHORIZATION_HEADER = Pattern.compile("(authorization|bearer)[\\s]*[:=]\\s*[\\w.]+", Pattern.CASE_INSENSITIVE);

  /**
   * Redact all secrets from an error message
   */
  public String redact(String message) {
    if (message == null || message.isEmpty()) {
      return "An error occurred";
    }

    String redacted = message;

    // Redact passwords in URLs (user:password@host)
    redacted = PASSWORD_IN_URL.matcher(redacted).replaceAll("$1:***@");

    // Redact JDBC connection strings
    redacted = JDBC_CONNECTION.matcher(redacted).replaceAll("jdbc:***");

    // Redact SSH key paths
    redacted = SSH_KEY_PATH.matcher(redacted).replaceAll("/***/.ssh/***");

    // Redact API keys and tokens
    redacted = API_KEY.matcher(redacted).replaceAll("$1***");

    // Redact password assignments
    redacted = PASSWORD_LITERAL.matcher(redacted).replaceAll("$1=***");

    // Redact authorization headers
    redacted = AUTHORIZATION_HEADER.matcher(redacted).replaceAll("$1:***");

    // Additional generic patterns
    redacted = redacted.replaceAll("(?i)secret[\\s]*=\\s*[\\w]+", "secret=***");
    redacted = redacted.replaceAll("(?i)token[\\s]*=\\s*[\\w.]+", "token=***");
    redacted = redacted.replaceAll("(?i)key[\\s]*=\\s*[\\w.]+", "key=***");

    return redacted;
  }

  /**
   * Redact secrets from exception message (for logging/display)
   */
  public String redactException(Throwable throwable) {
    if (throwable == null) {
      return "An error occurred";
    }

    String message = throwable.getMessage();
    if (message == null || message.isEmpty()) {
      message = throwable.getClass().getSimpleName();
    }

    return redact(message);
  }

  /**
   * Create safe error message from exception (human-readable, no secrets)
   */
  public String getSafeMessage(Throwable throwable) {
    String redacted = redactException(throwable);

    // Truncate if too long
    if (redacted.length() > 200) {
      redacted = redacted.substring(0, 197) + "...";
    }

    // If message is still too technical, provide generic fallback
    if (redacted.contains("null") || redacted.contains("NullPointerException") || redacted.isEmpty()) {
      return "An error occurred processing your request";
    }

    return redacted;
  }

}
