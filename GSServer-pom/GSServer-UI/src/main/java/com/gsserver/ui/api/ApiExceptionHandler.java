package com.gsserver.ui.api;

import com.gsserver.ui.hardening.HardeningExecutionException;
import com.gsserver.ui.hardening.PolicyViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
    ApiErrorResponse error = new ApiErrorResponse("INVALID_REQUEST", "Request body is malformed.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(PolicyViolationException.class)
  public ResponseEntity<ApiErrorResponse> handlePolicyViolation(PolicyViolationException exception) {
    ApiErrorResponse error = new ApiErrorResponse("POLICY_VIOLATION", exception.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
  }

  @ExceptionHandler(HardeningExecutionException.class)
  public ResponseEntity<ApiErrorResponse> handleExecutionFailure(HardeningExecutionException exception) {
    ApiErrorResponse error = new ApiErrorResponse("EXECUTION_FAILED", exception.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
    ApiErrorResponse error = new ApiErrorResponse("FORBIDDEN", "You are not allowed to perform this action.");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
    return ResponseEntity.status(status).body(new ApiErrorResponse(status.name(), message));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingResource(NoResourceFoundException exception) {
    ApiErrorResponse error = new ApiErrorResponse("NOT_FOUND", "Resource not found.");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedError(Exception exception) {
    ApiErrorResponse error = new ApiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
