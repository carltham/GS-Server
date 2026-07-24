package com.gsserver.ui.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Structured error response sent to API clients.
 *
 * All errors follow this format (never raw stack traces or exceptions).
 * Secrets are redacted before returning to client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

  private String errorId;
  private String code;
  private String message;
  private String timestamp;
  private String correlationId;
  private String severity;
  private String source;
  private Object details;
  private Boolean retryable;

  public ApiErrorResponse(String code, String message, String correlationId) {
    this.errorId = generateErrorId(code);
    this.code = code;
    this.message = message;
    this.timestamp = Instant.now().toString();
    this.correlationId = correlationId;
    this.severity = "ERROR";
    this.retryable = false;
  }

  public ApiErrorResponse(String code, String message, String correlationId, String source) {
    this(code, message, correlationId);
    this.source = source;
  }

  public ApiErrorResponse(String code, String message, String correlationId, String source, Object details) {
    this(code, message, correlationId, source);
    this.details = details;
  }

  private static String generateErrorId(String code) {
    return code + "_" + System.currentTimeMillis();
  }

  // Getters and setters
  public String getErrorId() {
    return errorId;
  }

  public void setErrorId(String errorId) {
    this.errorId = errorId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Object getDetails() {
    return details;
  }

  public void setDetails(Object details) {
    this.details = details;
  }

  public Boolean getRetryable() {
    return retryable;
  }

  public void setRetryable(Boolean retryable) {
    this.retryable = retryable;
  }

}
