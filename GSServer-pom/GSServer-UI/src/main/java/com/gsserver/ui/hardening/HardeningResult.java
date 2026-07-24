package com.gsserver.ui.hardening;

/**
 * Result of a hardening operation.
 *
 * Contains operation ID, status, and tenant/requester metadata.
 */
public class HardeningResult {

  private String operationId;
  private String tenantId;
  private String requestedBy;
  private String profile;
  private boolean success;
  private String message;
  private long timestamp;

  public HardeningResult(String operationId, String tenantId, String requestedBy,
                         String profile, boolean success) {
    this.operationId = operationId;
    this.tenantId = tenantId;
    this.requestedBy = requestedBy;
    this.profile = profile;
    this.success = success;
    this.timestamp = System.currentTimeMillis();
    this.message = success ? "Hardening operation completed" : "Hardening operation failed";
  }

  // Getters and setters
  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

}
