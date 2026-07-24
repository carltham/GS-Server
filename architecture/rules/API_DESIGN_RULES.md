# GS-Server API Design Rules

**Extracted from:** TARGET_API_STANDARDS.md  
**Last Updated:** 2026-07-24

---

## Rule 1: Define-Before-Implementing

**Public endpoints and schemas FIRST, implementation SECOND.**

- Design API contract before writing handlers
- Generate API documentation from code (OpenAPI 3.0)
- Detect breaking changes in CI/CD

---

## Rule 2: Consistent Error Model

**All errors use unified structure:**

```json
{
  "errorId": "HARDENING_FAILED_001",
  "code": "HARDENING_FAILED",
  "message": "Human-readable summary (non-technical)",
  "timestamp": "2026-07-24T10:00:00.000Z",
  "correlationId": "req-uuid-12345",
  "severity": "ERROR|WARN|INFO",
  "source": "ClassName.methodName()",
  "details": { /* error-specific context, NO SECRETS */ },
  "retryable": false,
  "documentationUrl": "https://docs.example.com/errors/CODE"
}
```

**Required fields:**
- errorId (unique error identifier)
- code (machine-readable, no spaces)
- message (human-readable)
- timestamp (ISO 8601 UTC)
- correlationId (trace across services)
- severity (ERROR, WARN, INFO)
- source (class/method that generated error)
- details (error context, no secrets)

---

## Rule 3: HTTP Method Semantics

| Method | Meaning | Idempotent | Use |
|--------|---------|-----------|-----|
| GET | Retrieve | Yes | Fetch data |
| POST | Create | No | New resource |
| PUT | Replace | Yes | Replace entire resource |
| PATCH | Update | No | Update subset |
| DELETE | Remove | Yes | Delete resource |

**GS-Server target:** Use PUT for replacements, PATCH for updates.

---

## Rule 4: Status Codes

**Always use semantically correct status codes:**

- 200 OK → Operation succeeded
- 201 Created → New resource created
- 202 Accepted → Long-running async accepted
- 400 Bad Request → Invalid input (DON'T USE for validation errors)
- 401 Unauthorized → No authentication
- 403 Forbidden → No authorization
- 404 Not Found → Resource missing
- 409 Conflict → Concurrent modification
- 422 Unprocessable → Validation failed (USE THIS for validation errors)
- 429 Too Many Requests → Rate limited
- 500 Server Error → Unexpected error
- 503 Unavailable → Service down

**Rule:** Use 422 for validation errors (not 400).

---

## Rule 5: Request Validation

**Validate at boundaries; validate business rules in Service.**

**Handler validates format:**
- ✅ Content-Type application/json
- ✅ Request body is valid JSON
- ✅ Required fields present
- ✅ Field types correct

**Service validates business rules:**
- ✅ Tenant authorization
- ✅ Operator authorization
- ✅ Value constraints (profile exists, port in range)

**Response for validation errors:**

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": {
    "errors": [
      {
        "field": "upstreamPort",
        "value": 99999,
        "constraint": "range",
        "expected": "1-65535"
      }
    ]
  }
}
```

---

## Rule 6: Response Format

**All successful responses:**
```json
{
  "data": { /* actual response */ },
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
  "error": { /* structured error object */ }
}
```

**Requirements:**
- Wrap all responses (data or error)
- Include correlation ID in all responses
- Include timestamp for all responses

---

## Rule 7: Authentication & Authorization

**Development:**
- HTTP Basic (username/password)
- Passwordless sudo for thor (localhost only)

**Production (Target):**
- OAuth2 with JWT tokens
- Scopes: hardening:read, hardening:write, proxy:*, firewall:*, etc.
- Token expiration: 1 hour access, 7 days refresh
- MFA for privileged operations

**API key fallback for service-to-service.**

---

## Rule 8: Pagination

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

## Rule 9: Versioning Strategy

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

## Rule 10: Rate Limiting

**Header format:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1626963600

If exceeded: 429 Too Many Requests
```

**Default limits:**
- Authenticated users: 100 requests/minute
- Unauthenticated: 10 requests/minute
- Hardening operations: 5/hour (slow process)
- Login attempts: 5/minute

---

## Pre-Ship API Checklist

- [ ] All endpoints return 200/201 on success
- [ ] All endpoints return appropriate 4xx on client error
- [ ] All error responses include errorId, code, message, details
- [ ] No secrets in error messages
- [ ] Correlation ID present in all responses
- [ ] Timestamp included in all responses
- [ ] Authorization verified (no cross-tenant access)
- [ ] Rate limiting respected (429 when exceeded)
- [ ] API documentation generated and current
- [ ] Breaking changes documented with sunset headers
- [ ] Field validation returns 422 with field details
- [ ] Pagination tested for large result sets

