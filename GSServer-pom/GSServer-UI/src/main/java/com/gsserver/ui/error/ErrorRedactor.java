package com.gsserver.ui.error;

/**
 * Redacts sensitive information from error messages to prevent credential leakage
 * in API responses while preserving full context in internal logs.
 */
public class ErrorRedactor {

  private static final String REDACTION_MASK = "***";

  /**
   * Redacts passwords from JDBC/database URLs.
   * Example: "jdbc:mysql://user:password@host" → "jdbc:mysql://user:***@host"
   */
  public String redactPasswordsFromUrl(String input) {
    if (input == null) return null;
    // Match pattern: ://username:password@host
    return input.replaceAll("(://[^:/@]+:)[^@]+([@])", "$1" + REDACTION_MASK + "$2");
  }

  /**
   * Redacts file paths and sensitive file names.
   * Example: "/home/user/.ssh/id_rsa" → "/home/user/.ssh/***"
   */
  public String redactFilePaths(String input) {
    if (input == null) return null;
    // Match sensitive file patterns
    return input
      .replaceAll("(\\.ssh/)[^\\s/]+", "$1" + REDACTION_MASK)
      .replaceAll("(\\.aws/)[^\\s/]+", "$1" + REDACTION_MASK)
      .replaceAll("(\\.kube/)[^\\s/]+", "$1" + REDACTION_MASK)
      .replaceAll("(/etc/passwd)", REDACTION_MASK)
      .replaceAll("(/etc/shadow)", REDACTION_MASK);
  }

  /**
   * Redacts process output that may contain API keys, tokens, or credentials.
   * Matches patterns like: API_KEY=value, token=value, Bearer <token>
   */
  public String redactProcessOutput(String output) {
    if (output == null) return null;
    return output
      // Match API_KEY=..., api_key=..., API-KEY=...
      .replaceAll("(?i)(api[_-]?key|secret|password|passwd|pwd)\\s*[=:]\\s*[^\\s;,]+", "$1=" + REDACTION_MASK)
      // Match Bearer tokens
      .replaceAll("(?i)(bearer|authorization)\\s+[^\\s]+", "$1 " + REDACTION_MASK)
      // Match JWT tokens (ey...==)
      .replaceAll("ey[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", REDACTION_MASK)
      // Match AWS keys (AKIA...)
      .replaceAll("AKIA[0-9A-Z]{16}", REDACTION_MASK);
  }

  /**
   * Redacts database connection strings and credentials.
   * Example: "Failed to connect: jdbc:mysql://user:pass@db" → "Failed to connect: jdbc:mysql://user:***@db"
   */
  public String redactDatabaseCredentials(String input) {
    if (input == null) return null;
    return redactPasswordsFromUrl(input);
  }

  /**
   * Comprehensive redaction of all sensitive patterns.
   * Applies all redaction methods to ensure no credentials leak.
   */
  public String redact(String errorMessage) {
    if (errorMessage == null) return null;

    String result = errorMessage;
    result = redactPasswordsFromUrl(result);
    result = redactFilePaths(result);
    result = redactProcessOutput(result);
    result = redactDatabaseCredentials(result);

    return result;
  }
}
