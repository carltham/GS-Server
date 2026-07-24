package com.gsserver.ui.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured success response sent to API clients.
 *
 * All successful responses follow this format with data + metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse {

  private Object data;
  private Map<String, Object> metadata;

  public ApiSuccessResponse(Object data, String correlationId) {
    this.data = data;
    this.metadata = new LinkedHashMap<>();
    this.metadata.put("timestamp", Instant.now().toString());
    this.metadata.put("correlationId", correlationId);
    this.metadata.put("version", "1.0");
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

}
