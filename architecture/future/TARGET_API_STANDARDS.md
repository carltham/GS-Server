# GS-Server Target API Standards

**Project:** GS-Server  
**Version:** 1.0 (Target State)  
**Last Updated:** 2026-07-24  
**Based on:** Organizational standards from `/mnt/DATA/WORKSPACE/project-settings/common/development-rules/API_DESIGN.md`

---

## Core Principle: Define Before Implementing

**Public endpoints and schemas FIRST, implementation SECOND.**

- Design API contract before writing handlers
- Generate API documentation from code (OpenAPI 3.0)
- Detect breaking changes in CI/CD

---

## REST API Standards

### Endpoint Design

**Define endpoints with:**
- Clear HTTP method (GET, POST, PUT, PATCH, DELETE)
- Resource-oriented naming (nouns, not verbs)
- Consistent versioning (v1, v2 in path)
- Request/response schemas in OpenAPI

**Example - Hardening endpoint:**
```
POST /api/v1/hardening
  Request: HardeningRequest (tenantId, profile, requestedBy)
  Response: HardeningResponse (202 Accepted, operationId)
  Authorization: @PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
```

### Consistent Error Model (MANDATORY)

**All errors use unified structure:**

```json
{
  "error": {
    "errorId": "HARDENING_FAILED_001",
    "code": "HARDENING_FAILED",
    "message": "Hardening operation failed (check logs for details)",
    "timestamp": "2026-07-24T10:00:00.000Z",
    "correlationId": "req-uuid-12345",
    "severity": "ERROR|WARN|INFO",
    "source": "com.gsserver.ui.hardening.DefaultHardeningService.triggerHardening()",
    "details": {
      "exitCode": 1,
      "timedOut": false,
      "platform": "linux"
    },
    "retryable": false,
    "documentationUrl": "https://docs.gsserver.local/errors/HARDENING_FAILED"
  }
}
```

**Required fields:**
- `errorId`: Unique error identifier (CAPABILITY_ERROR_NUMBER)
- `code`: Machine-readable, no spaces
- `message`: Human-readable summary (NO SECRETS)
- `timestamp`: ISO 8601 UTC
- `correlationId`: Trace across services
- `severity`: ERROR, WARN, INFO
- `source`: ClassName.methodName()
- `details`: Error context (NO SECRETS)
- `retryable`: Can client retry safely?
- `documentationUrl`: Link to error documentation

**Current state:** ❌ Generic error model only (500 + message)
**Target state:** ✅ Structured error model by Phase 2

---

### HTTP Method Semantics

| Method | Meaning | Idempotent | Use |
|--------|---------|-----------|-----|
| GET | Retrieve | Yes | Fetch data (read-only) |
| POST | Create | No | New resource, async operations |
| PUT | Replace | Yes | Replace entire resource |
| PATCH | Update | No | Update subset of resource |
| DELETE | Remove | Yes | Delete resource |

**GS-Server conventions:**
- POST for state-changing operations (hardening, proxy config)
- 202 Accepted for async/long-running ops
- 200 OK for synchronous operations
- GET for read-only queries

---

### HTTP Status Codes (Complete Set)

| Code | Meaning | Use | GS-Server Examples |
|---|---|---|---|
| 200 | OK | Synchronous success | GET /api/v1/users (list) |
| 201 | Created | New resource created | POST /api/v1/users (created user) |
| 202 | Accepted | Async op started | POST /api/v1/hardening (operation in progress) |
| 204 | No Content | Success, no response body | DELETE /api/v1/users/{id} |
| 400 | Bad Request | Malformed JSON | Invalid JSON syntax |
| 401 | Unauthorized | No authentication | Missing Authorization header |
| 403 | Forbidden | No authorization | @PreAuthorize check failed |
| 404 | Not Found | Resource missing | GET /api/v1/users/{id} (not found) |
| 409 | Conflict | Concurrent modification | Two simultaneous config changes |
| 422 | Unprocessable | Validation failed | POST with invalid profile name |
| 429 | Too Many Requests | Rate limited | Too many login attempts |
| 500 | Server Error | Unexpected error | Unhandled exception |
| 503 | Unavailable | Service down | Maintenance mode |

**Critical rule:** Use 422 for VALIDATION errors (not 400).

---

### Request Validation (Layered)

**Validation happens in two layers:**

**Layer 1: Handler (Format validation)**
- ✅ Content-Type application/json
- ✅ Request body is valid JSON (Spring @RequestBody)
- ✅ Required fields present (Spring @NotNull)
- ✅ Field types correct (Spring @NotNull, @Min, @Max)
- ❌ Business logic (tenant exists, profile valid)

**Layer 2: Service (Business rule validation)**
- ✅ Tenant authorized for this operation
- ✅ Operator authorized for this role
- ✅ Value constraints (profile in ALLOWED_PROFILES)
- ✅ Cross-tenant access (validate in both layers)

**Response for validation errors (422):**
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "timestamp": "2026-07-24T10:00:00.000Z",
    "details": {
      "errors": [
        {
          "field": "upstreamPort",
          "value": 99999,
          "constraint": "range",
          "expected": "1-65535",
          "message": "Port must be between 1 and 65535"
        }
      ]
    }
  }
}
```

---

### Response Format (Consistent Wrapper)

**All successful responses:**
```json
{
  "data": {
    "operationId": "uuid",
    "status": "success"
  },
  "metadata": {
    "timestamp": "2026-07-24T10:00:00.000Z",
    "correlationId": "req-uuid-12345",
    "version": "1.0"
  }
}
```

**All error responses:**
```json
{
  "error": {
    "errorId": "HARDENING_FAILED_001",
    "code": "HARDENING_FAILED",
    "message": "...",
    ...
  }
}
```

**Requirements:**
- Wrap all responses (data or error)
- Include correlationId in every response
- Include timestamp in every response
- Consistent field names across endpoints

---

### Authentication & Authorization

**Development (current):**
- ✅ HTTP Basic (username/password)
- ✅ Passwordless sudo for thor (localhost only)

**Production (target):**
- ✅ OAuth2 with JWT tokens (Phase 6)
- ✅ Scopes: hardening:read, hardening:write, proxy:*, firewall:*
- ✅ Token expiration: 1 hour access, 7 days refresh
- ✅ MFA for privileged operations (Phase 6+)

**API key fallback for service-to-service (Phase 3+)**

**Mandatory rule:** Validate authorization in BOTH API layer AND Service layer (never trust UI alone).

---

### Pagination (When Needed)

**When:** Lists over 100 items

**Cursor-based (preferred):**
```json
{
  "data": [ /* items */ ],
  "pagination": {
    "cursor": "next-cursor-token",
    "hasMore": true,
    "limit": 50
  }
}
```

**Offset-based (fallback):**
```json
{
  "data": [ /* items */ ],
  "pagination": {
    "offset": 0,
    "limit": 50,
    "total": 1234,
    "pages": 25
  }
}
```

---

### Versioning Strategy

**URL path versioning:**
- Major breaking changes in URL path (v1, v2)
- Minor additive changes in request/response without version bump
- 2 versions supported simultaneously (current + previous)
- Deprecation header 6 months before sunset

```
X-API-Deprecation-Date: 2026-12-24
X-API-Sunset: 2027-01-24
```

---

### Rate Limiting

**Header format:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1626963600

If exceeded: 429 Too Many Requests
```

**Default limits (target):**
- Authenticated users: 100 requests/minute
- Unauthenticated: 10 requests/minute
- Hardening operations: 5/hour (slow process)
- Login attempts: 5/minute

---

### Secrets Protection in API (MANDATORY)

**Never include in responses:**
- ❌ API keys or tokens
- ❌ Passwords (even hashed, unless readonly)
- ❌ SSH private keys
- ❌ Database credentials
- ❌ Personal data (email, phone, addresses)

**Error messages (user-facing):**
```
❌ BAD: "Failed to connect to server 192.168.1.100:22: permission denied for user root"
✅ GOOD: "Failed to connect to server (check network and credentials)"
```

**Internal logs (behind auth):**
```
✅ OK: "Failed SSH connection: host=192.168.1.100, user=root, error=permission_denied"
        (Logs are behind authentication, but still redact sensitive details)
```

---

## API Checklist (Pre-Ship)

- [ ] All endpoints documented in OpenAPI 3.0
- [ ] All endpoints return 200/201/202 on success
- [ ] All endpoints return appropriate 4xx on client error
- [ ] All error responses include errorId, code, message, details
- [ ] No secrets in error messages (automated scan done)
- [ ] Correlation ID present in all responses
- [ ] Timestamp included in all responses
- [ ] Authorization verified in both handler and service
- [ ] Cross-tenant access tested and denied
- [ ] Rate limiting configured and tested (429 when exceeded)
- [ ] Breaking changes documented with sunset headers
- [ ] Field validation returns 422 with field details
- [ ] Pagination tested for large result sets
- [ ] Contracts tested for all status codes

---

## Summary

**Target API standards for GS-Server:**
- ✅ Define endpoints before implementation
- ✅ Use unified structured error model
- ✅ Consistent HTTP semantics
- ✅ Layered validation (format + business rules)
- ✅ Wrapped responses (all data or all error)
- ✅ No secrets in API responses
- ✅ Authorization in both layers
- ✅ Rate limiting and pagination
- ✅ Semantic versioning

**Current gap:** Generic error model (500 + message)
**Phase 2 target:** Structured error model with errorId, code, details

