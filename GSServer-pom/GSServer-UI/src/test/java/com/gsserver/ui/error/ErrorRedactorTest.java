package com.gsserver.ui.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for ErrorRedactor to ensure all sensitive patterns are redacted.
 * Covers: passwords, file paths, API keys, tokens, database credentials.
 */
public class ErrorRedactorTest {

  private ErrorRedactor redactor = new ErrorRedactor();

  // ===== Password Redaction Tests =====

  @Test
  public void redactPasswordFromMysqlUrl() {
    String input = "jdbc:mysql://admin:SecurePass123@db.example.com:3306/mydb";
    String result = redactor.redactPasswordsFromUrl(input);

    assertThat(result).contains("***");
    assertThat(result).doesNotContain("SecurePass123");
    assertThat(result).contains("admin");
    assertThat(result).contains("db.example.com");
  }

  @Test
  public void redactPasswordFromPostgresUrl() {
    String input = "jdbc:postgresql://user:mypassword@localhost:5432/database";
    String result = redactor.redactPasswordsFromUrl(input);

    assertThat(result).contains("user:***@");
    assertThat(result).doesNotContain("mypassword");
  }

  @Test
  public void redactPasswordFromHttpsUrl() {
    String input = "https://admin:secretpassword@api.example.com/endpoint";
    String result = redactor.redactPasswordsFromUrl(input);

    assertThat(result).contains("admin:***@");
    assertThat(result).doesNotContain("secretpassword");
  }

  @Test
  public void handleNullUrlGracefully() {
    String result = redactor.redactPasswordsFromUrl(null);
    assertThat(result).isNull();
  }

  // ===== File Path Redaction Tests =====

  @Test
  public void redactSshKeyPath() {
    String input = "SSH key not found at /home/thor/.ssh/id_rsa";
    String result = redactor.redactFilePaths(input);

    assertThat(result).contains(".ssh/***");
    assertThat(result).doesNotContain("id_rsa");
    assertThat(result).contains("/home/thor");
  }

  @Test
  public void redactAwsCredentialsFile() {
    String input = "Failed to read /root/.aws/credentials file";
    String result = redactor.redactFilePaths(input);

    assertThat(result).contains(".aws/***");
    assertThat(result).doesNotContain("credentials");
  }

  @Test
  public void redactKubeConfigPath() {
    String input = "Error loading /home/user/.kube/config";
    String result = redactor.redactFilePaths(input);

    assertThat(result).contains(".kube/***");
    assertThat(result).doesNotContain("config");
  }

  @Test
  public void redactEtcPasswdPath() {
    String input = "Permission denied: /etc/passwd";
    String result = redactor.redactFilePaths(input);

    assertThat(result).contains("***");
    assertThat(result).doesNotContain("/etc/passwd");
  }

  @Test
  public void redactEtcShadowPath() {
    String input = "Cannot access /etc/shadow";
    String result = redactor.redactFilePaths(input);

    assertThat(result).contains("***");
    assertThat(result).doesNotContain("/etc/shadow");
  }

  // ===== Process Output / API Key Redaction Tests =====

  @Test
  public void redactApiKeyFromProcessOutput() {
    String input = "Error: API_KEY=sk_live_abc123xyz failed authorization";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("API_KEY=***");
    assertThat(result).doesNotContain("sk_live_abc123xyz");
  }

  @Test
  public void redactApiKeyWithDash() {
    String input = "Config: api-key=secret-value-here";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("api-key=***");
    assertThat(result).doesNotContain("secret-value-here");
  }

  @Test
  public void redactPasswordField() {
    String input = "Password=MyPassword123 in connection string";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("Password=***");
    assertThat(result).doesNotContain("MyPassword123");
  }

  @Test
  public void redactBearerToken() {
    String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("Bearer ***");
    assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
  }

  @Test
  public void redactJwtToken() {
    String input = "Invalid token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("Invalid token: ***");
    assertThat(result).doesNotContain("eyJhbGci");
  }

  @Test
  public void redactAwsAccessKey() {
    String input = "AWS credentials: AKIAIOSFODNN7EXAMPLE:wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    String result = redactor.redactProcessOutput(input);

    assertThat(result).contains("***");
    assertThat(result).doesNotContain("AKIAIOSFODNN7EXAMPLE");
  }

  // ===== Database Credentials Tests =====

  @Test
  public void redactDatabaseCredentials() {
    String input = "Failed to connect: jdbc:mysql://root:admin123@localhost:3306/production";
    String result = redactor.redactDatabaseCredentials(input);

    assertThat(result).contains("root:***@");
    assertThat(result).doesNotContain("admin123");
  }

  // ===== Comprehensive Redaction Tests =====

  @Test
  public void redactAllPatterns() {
    String input = "Database error: jdbc:mysql://user:pass@db. SSH key: /root/.ssh/id_rsa. Token: Bearer token123. API_KEY=sk_live_123";
    String result = redactor.redact(input);

    assertThat(result).doesNotContain("pass");
    assertThat(result).doesNotContain("id_rsa");
    assertThat(result).doesNotContain("token123");
    assertThat(result).doesNotContain("sk_live_123");
    assertThat(result).contains("***");
  }

  @Test
  public void redactMultipleOccurrences() {
    String input = "password=secret1 and password=secret2 and API_KEY=key123";
    String result = redactor.redact(input);

    assertThat(result).doesNotContain("secret1");
    assertThat(result).doesNotContain("secret2");
    assertThat(result).doesNotContain("key123");
  }

  @Test
  public void handleNullGracefully() {
    String result = redactor.redact(null);
    assertThat(result).isNull();
  }

  @Test
  public void handleEmptyStringGracefully() {
    String result = redactor.redact("");
    assertThat(result).isEmpty();
  }

  @Test
  public void preserveHumanReadableMessage() {
    String input = "Database connection failed: jdbc:mysql://user:password@db";
    String result = redactor.redact(input);

    assertThat(result).contains("Database connection failed");
    assertThat(result).contains("jdbc:mysql");
    assertThat(result).contains("user");
  }

  @Test
  public void redactCaseInsensitive() {
    String input = "ERROR: PASSWORD=secret123 api_KEY=key456 Secret=mysecret";
    String result = redactor.redact(input);

    assertThat(result).doesNotContain("secret123");
    assertThat(result).doesNotContain("key456");
    assertThat(result).doesNotContain("mysecret");
  }

  @Test
  public void handleComplexStackTrace() {
    String input = "Exception in thread \"main\"\n" +
      "java.sql.SQLException: Access denied for user 'admin'@'localhost' (using password: YES)\n" +
      "  at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1073)\n" +
      "  Caused by: Connection string jdbc:mysql://admin:dangerouspass@db:3306/prod";

    String result = redactor.redact(input);

    assertThat(result).doesNotContain("dangerouspass");
    assertThat(result).contains("admin");
    assertThat(result).contains("SQLException");
  }
}
