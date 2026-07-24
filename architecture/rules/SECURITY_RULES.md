# GS-Server Security Rules

**Extracted from:** TARGET_SECURITY_POLICY.md  
**Last Updated:** 2026-07-24

---

## Rule 1: Authentication Required for All Operations

**Policy:** Every API endpoint must authenticate the request.

**Development:**
- HTTP Basic Auth (username/password)
- Passwordless sudo for `thor` user (localhost only)

**Production (Target):**
- OAuth2 with JWT tokens
- Scopes: `hardening:read`, `hardening:write`, `proxy:*`, `firewall:*`
- Token expiration: 1 hour access, 7 days refresh
- MFA for privileged operations

**Endpoint enforcement:**
```java
@Controller
@RequestMapping("/api/v1/hardening")
public class HardeningHandler {
  @PostMapping
  @PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    // ✅ Only authenticated users with GROUP_HARDENING_OPERATORS can access
    return ResponseEntity.ok(service.harden(req));
  }
}
```

---

## Rule 2: Never Trust UI-Only Authorization

**Policy:** Validate authorization at BOTH API layer AND Service layer.

**Pattern:**
```java
// Layer 1: API/Handler validation
@Controller
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public class HardeningHandler {
  @PostMapping
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    // ← Handler verified user has GROUP_HARDENING_OPERATORS
    return ResponseEntity.ok(service.harden(req));
  }
}

// Layer 2: Service validation (can't trust handler)
@Service
public class DefaultHardeningService {
  public HardeningResult harden(HardeningRequest req) {
    // ← Service validates tenant access again
    validateTenantAccess(req.tenantId());
    // ← Service validates operator access again
    validateOperatorAccess(req.requestedBy());
    return adapter.execute(req);
  }
}
```

**Why:** Frontend can be spoofed, network traffic can be intercepted, handler can be bypassed. Service layer is always the source of truth.

---

## Rule 3: Multi-Tenant Isolation

**Policy:** Tenant data is completely isolated; no cross-tenant access.

**Implementation:**
- Every operation includes tenant ID
- Service validates tenant access before execution
- Repository queries scoped to tenant
- Logs include tenant ID for audit trail

**Code example:**
```java
@Service
public class DefaultHardeningService {
  public List<HardeningResult> listOperations(String tenantId, User actor) {
    // Validate tenant access
    validateTenantAccess(tenantId);
    
    // Scoped query: only return operations for this tenant
    return repository.findByTenant(tenantId);
  }
}
```

**Test requirement:**
```java
@Test
public void userFromTenantA_cannotAccess_tenantB_data() {
  User userA = createUserInTenant(TENANT_A);
  OperationState dataB = createOperationInTenant(TENANT_B);
  
  assertThatThrownBy(() ->
    service.getOperationState(dataB.id(), userA))
    .isInstanceOf(AccessDeniedException.class);
}
```

---

## Rule 4: Secrets Redaction

**Policy:** No credentials, API keys, or sensitive data in output.

**Redacted fields:**
- ✅ Database passwords
- ✅ API keys
- ✅ SSH private keys
- ✅ OAuth tokens
- ✅ Personal data (email, phone, addresses)

**In error messages:**
```java
// ❌ BAD: exposes secret
catch (Exception e) {
  return ResponseEntity.status(500)
    .body("Failed to connect to database: " + dbPassword);
}

// ✅ GOOD: redacts secret
catch (Exception e) {
  log.error("Database connection failed", e);  // Logs contain secret
  return ResponseEntity.status(500)
    .body("Failed to connect to database");      // Response is safe
}
```

**In logs:**
```java
// ✅ GOOD: redact before logging
String redactedPassword = password.replaceAll(".", "*");
log.info("Authentication attempt for user={}, password={}", 
  username, redactedPassword);
```

**Audit requirement:**
- Scan all error messages: `grep -r "password\|secret\|key\|token" src/`
- Scan all logs: ensure no secrets appear
- Manual review of sensitive error paths

---

## Rule 5: Data Encryption at Rest

**Current state:**
- File-based JSON (plaintext)

**Target state:**
- SQLite with encryption-at-rest
- All sensitive fields encrypted
- Encryption keys managed separately

**Timeline:** Phase 6 (observability phase)

---

## Rule 6: HTTPS/TLS for All Connections

**Development:**
- HTTP allowed (localhost only)
- Self-signed certificates OK

**Production (Target):**
- HTTPS mandatory
- TLS 1.2+ (no older versions)
- Certificate validity checked
- HSTS header: `Strict-Transport-Security: max-age=31536000`

**Spring Boot config:**
```properties
# Production
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

---

## Rule 7: Audit Logging (Mandatory)

**Policy:** Every operation logged with actor, timestamp, and action.

**Audit log fields (required):**
- `timestamp` - ISO 8601 UTC
- `actor` - User who performed action
- `tenantId` - Tenant being acted upon
- `operation` - What was done (e.g., "HARDEN", "PROXY_CONFIG")
- `status` - SUCCESS, FAILED
- `details` - Operation-specific data (NO secrets)
- `correlationId` - Trace across requests

**Example:**
```json
{
  "timestamp": "2026-07-24T10:00:00.000Z",
  "actor": "operator@company.com",
  "tenantId": "tenant-xyz",
  "operation": "HARDEN_SERVER",
  "status": "SUCCESS",
  "details": {
    "serverId": "server-123",
    "profile": "strict"
  },
  "correlationId": "req-uuid-12345"
}
```

**Storage:**
- Append-only file (cannot be modified)
- Retention: 1+ year
- No access to modify/delete logs
- Regular backups

---

## Rule 8: Compliance & Incident Response

**Compliance requirements:**
- SOC2 Type II audit trail
- HIPAA encryption at rest
- GDPR right-to-be-forgotten (deletes user data + audit entries)

**Incident response:**
- Security team notified within 1 hour of breach detection
- Affected users notified within 24 hours
- Root cause analysis required
- Lessons learned applied to code

---

## Rule 9: Role-Based Access Control (RBAC)

**Current state:**
- 4 role groups: VIEWER, OPERATOR, ADMIN, SUPER_ADMIN

**Role capabilities:**

| Role | Hardening | Proxy | Firewall | Users |
|---|---|---|---|---|
| **VIEWER** | Read-only | Read-only | Read-only | Read-only |
| **OPERATOR** | Read+Write | Read+Write | Read+Write | None |
| **ADMIN** | All | All | All | Manage |
| **SUPER_ADMIN** | All | All | All | All + Audit |

**Enforcement:**
```java
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public ResponseEntity<?> harden(HardeningRequest req) {
  // Only OPERATOR+ can execute
}

@PreAuthorize("hasAuthority('GROUP_ADMIN')")
public ResponseEntity<?> createUser(UserRequest req) {
  // Only ADMIN+ can create users
}
```

---

## Rule 10: Security Review Checklist

**Before every release:**

- [ ] Authentication enforced (no anonymous endpoints)
- [ ] Authorization checked in handler AND service
- [ ] Cross-tenant access denied (tests passing)
- [ ] No secrets in error messages (grep scan done)
- [ ] No secrets in logs (audit trail reviewed)
- [ ] Audit logging working (sample logs verified)
- [ ] HTTPS enforced (production)
- [ ] TLS 1.2+ (no older versions)
- [ ] Encryption at rest (where applicable)
- [ ] Correlation IDs in all responses
- [ ] Rate limiting enforced (429 on limit)
- [ ] Input validation present (400 or 422 on invalid)
- [ ] SQL injection prevented (parameterized queries)
- [ ] XSS prevented (output encoding)
- [ ] CSRF tokens used (state-changing operations)
- [ ] Dependency vulnerabilities scanned (mvn dependency-check)
- [ ] Code reviewed by security team
- [ ] Security tests updated

---

## Common Security Mistakes & Prevention

| Mistake | Prevention | Status |
|---|---|---|
| Auth only in handler | Validate in service too | ✅ Implemented |
| Cross-tenant access | Cross-tenant tests | ❌ Missing tests |
| Secrets in error messages | Redaction audit | 🟨 Partial |
| Secrets in logs | Log redaction review | 🟨 Partial |
| No audit trail | Mandatory logging | ❌ Missing logging |
| SQL injection | Parameterized queries | ✅ Implemented |
| XSS | Output encoding | ✅ Implemented |
| CSRF | Token validation | ✅ Implemented |
| Weak passwords | Bcrypt + salt | ✅ Implemented |
| Expired tokens | Token validation | ✅ Implemented |

