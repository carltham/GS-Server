package com.gsserver.ui.common;

import org.slf4j.MDC;
import java.util.UUID;

/**
 * Utility for generating and managing correlation IDs for request tracing.
 *
 * Correlation ID allows tracking a request through all layers (Handler → Service → Adapter → External).
 * Used for distributed tracing and debugging.
 */
public class CorrelationIdUtil {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String MDC_CORRELATION_ID = "correlationId";

  /**
   * Generate a new unique correlation ID (UUID format)
   */
  public static String generateCorrelationId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Set correlation ID in MDC (Mapped Diagnostic Context) for logging
   */
  public static void setInMdc(String correlationId) {
    if (correlationId != null) {
      MDC.put(MDC_CORRELATION_ID, correlationId);
    }
  }

  /**
   * Get correlation ID from MDC
   */
  public static String getFromMdc() {
    return MDC.get(MDC_CORRELATION_ID);
  }

  /**
   * Clear correlation ID from MDC
   */
  public static void clearFromMdc() {
    MDC.remove(MDC_CORRELATION_ID);
  }

  /**
   * Get header name for correlation ID
   */
  public static String getHeaderName() {
    return CORRELATION_ID_HEADER;
  }

}
