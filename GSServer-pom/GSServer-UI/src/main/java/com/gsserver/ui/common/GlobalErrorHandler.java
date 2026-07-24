package com.gsserver.ui.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for all API endpoints.
 *
 * Catches exceptions, redacts secrets, and returns structured error responses.
 * All errors are logged internally with full context, but API responses are sanitized.
 */
@RestControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);

  private final ErrorRedactor errorRedactor;

  public GlobalErrorHandler(ErrorRedactor errorRedactor) {
    this.errorRedactor = errorRedactor;
  }

  /**
   * Handle all exceptions - catch-all handler
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
    // Get correlation ID from MDC (set by Controller)
    String correlationId = CorrelationIdUtil.getFromMdc();
    if (correlationId == null) {
      correlationId = CorrelationIdUtil.generateCorrelationId();
    }

    // Log full error internally (with secrets, stack trace, etc.)
    logger.error("Exception in request [{}]: {}", correlationId, ex.getMessage(), ex);

    // Create safe error response (redacted, no stack trace)
    String safeMessage = errorRedactor.getSafeMessage(ex);
    String errorCode = "INTERNAL_ERROR";

    ApiErrorResponse errorResponse = new ApiErrorResponse(
        errorCode,
        safeMessage,
        correlationId,
        ex.getClass().getName()
    );

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }

  /**
   * Handle validation exceptions
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(IllegalArgumentException ex) {
    String correlationId = CorrelationIdUtil.getFromMdc();
    if (correlationId == null) {
      correlationId = CorrelationIdUtil.generateCorrelationId();
    }

    logger.warn("Validation error [{}]: {}", correlationId, ex.getMessage());

    String safeMessage = errorRedactor.getSafeMessage(ex);
    ApiErrorResponse errorResponse = new ApiErrorResponse(
        "VALIDATION_FAILED",
        safeMessage,
        correlationId,
        ex.getClass().getName()
    );

    return ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(errorResponse);
  }

  /**
   * Handle access denied exceptions
   */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiErrorResponse> handleSecurityException(SecurityException ex) {
    String correlationId = CorrelationIdUtil.getFromMdc();
    if (correlationId == null) {
      correlationId = CorrelationIdUtil.generateCorrelationId();
    }

    logger.warn("Security error [{}]: {}", correlationId, ex.getMessage());

    ApiErrorResponse errorResponse = new ApiErrorResponse(
        "ACCESS_DENIED",
        "Access denied",
        correlationId,
        ex.getClass().getName()
    );

    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(errorResponse);
  }

}
