# GS-Server Target Security Policy

**Project:** GS-Server  
**Version:** 1.0 (Target State)  
**Last Updated:** 2026-07-24  
**Based on:** Organizational standards from `common/development-rules/SECURITY_AND_ISOLATION.md`

---

## Executive Summary

GS-Server manages critical server operations (hardening, proxy, firewall, deployments). Security is built-in, not added later. We implement defense-in-depth across all layers.

---

## Core Security Requirements

### 1. Authentication (WHO ARE YOU?)

**Development (current):**
- HTTP Basic (username/password)
- Passwordless sudo for thor (localhost only)

**Production (target):**
- OAuth2 with JWT tokens (Phase 6)
- Scopes: hardening:read, hardening:write, proxy:*
- Token expiration: 1 hour access, 7 days refresh
- MFA for privileged operations (Phase 6+)

**Mandatory:** Every API endpoint requires authentication. No anonymous access (except auth config).

---

### 2. Authorization (CAN YOU DO THIS?)

**Mandatory rule:** Validate authorization in BOTH API layer AND Service layer.

**Implementation pattern:**
```java
// Handler validates format
@RestController
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public class HardeningHandler {
  @PostMapping("/harden")
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    return ResponseEntity.ok(service.harden(req));
  }
}

// Service validates again (can't trust handler)
@Service
public class DefaultHardeningService {
  public HardeningResult harden(HardeningRequest req) {
    validateTenantAccess(req.tenantId());  // Validate again
    validateOperatorAccess(req.requestedBy());  // Validate again
    return adapter.execute(req);
  }
}
```

**Why both layers?**
- Frontend can be bypassed
- API can be called from scripts/tools that skip handler
- Service layer is always the source of truth

---

### 3. Multi-Tenant Isolation (MANDATORY, TESTED)

**Core rule:** Reject cross-tenant access BEFORE repository access.

**Requirements:**
- Every operation includes tenantId
- Service validates tenant access before execution
- Repository queries scoped to tenant
- Audit logs include tenant context
- **CRITICAL:** Cross-tenant denial tests (verify authorization)

**Test requirement (MANDATORY):**
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

**Current state:** ⚠️ Validation in place, NO tests for denial
**Target state:** ✅ Validation + comprehensive denial tests by Phase 2

---

### 4. Secrets Redaction (MANDATORY, CRITICAL GAP)

**Rule:** Never expose secrets in errors, logs, events, or API responses.

**Secrets redaction (Phase 1 - BLOCKING):**

| Location | Current | Target | By |
|---|---|---|---|
| Error messages | ❌ May leak | ✅ Generic message | Phase 1 |
| API responses | ❌ Never safe | ✅ Never | Phase 1 |
| Logs | ❌ Passwords may appear | ✅ Redact sensitive | Phase 1 |
| Events/audit | ❌ Not logged yet | ✅ Logged securely | Phase 1 |

**Example - BAD:**
```
❌ "Failed to connect to database: user=admin password=secret123"
❌ "SSH key not found at /home/thor/.ssh/id_rsa"
❌ Process output: "Warning: using default password 'hardcoded-secret'"
```

**Example - GOOD:**
```
✅ "Failed to connect to database (check network configuration)"
✅ "SSH key not found (contact administrator)"
✅ "Configuration loaded successfully"
  [Internal log: "SSH key missing from /home/thor/.ssh/id_rsa"]
```

**Implementation strategy:**
- Scan error messages before commit (secrets detection tool)
- Redact credentials from log output
- Never echo shell commands containing secrets
- Use placeholder values in examples/docs

---

### 5. Audit Logging (MANDATORY, MANDATORY, MANDATORY)

**Rule:** Record the actor and correlation context for EVERY write operation.

**Audit trail (Phase 1 - BLOCKING):**

Every write operation must log:
```json
{
  "timestamp": "2026-07-24T10:00:00.000Z",
  "correlationId": "req-uuid-12345",
  "actor": "operator@company.com",
  "tenantId": "tenant-a",
  "action": "hardening.apply",
  "resource": "server-prod-01",
  "result": "success|failed",
  "details": {
    "profile": "strict",
    "platform": "Linux",
    "exitCode": 0
  }
}
```

**Mandatory events to audit:**
- Authentication events (login, logout, MFA)
- Permission failures (403 Forbidden)
- All write operations (POST, PUT, PATCH, DELETE)
- Imports/uploads
- Privileged access
- Configuration changes
- Rollback operations

**Retention:**
- Audit logs: 1 year minimum
- Operation history: Forever (immutable)
- Error logs: 30 days

**Current state:** ❌ No audit logging
**Target state:** ✅ Complete audit trail by Phase 1 (BLOCKING)

---

### 6. Encryption & Data Protection

**At Rest (Development → Production evolution):**
- Phase 1: File permissions 0600 (readable by app only)
- Phase 3: SQLite with SQLCipher encryption
- Phase 6: Backup encryption AES-256

**In Transit:**
- Development: HTTP OK (localhost)
- Production (target): HTTPS/TLS 1.2+ (enforce)
- Future: TLS 1.3, certificate pinning

**Current state:** ⚠️ File permissions set, no encryption yet
**Target state:** ✅ Encrypted at rest by Phase 3, TLS enforced in Phase 2

---

### 7. Input Validation (Prevent Injection)

**Layered validation:**
1. **Handler (format):** JSON structure, types, required fields
2. **Service (business rules):** Tenant valid, profile valid, port in range
3. **Adapter (safe execution):** Use only validated parameters

**Pattern:**
```java
// Handler validates format (Spring @RequestBody + annotations)
@PostMapping
public ResponseEntity<?> harden(
    @RequestBody @Valid HardeningRequest request) {  // Spring validates
  // Service validates business rules
  service.harden(request);
}

@Service
public class DefaultHardeningService {
  public void harden(HardeningRequest request) {
    if (!ALLOWED_PROFILES.contains(request.profile())) {
      throw new PolicyViolationException("Invalid profile");  // Business validation
    }
    adapter.execute(request);  // Execute only validated request
  }
}
```

**Current state:** ✅ Validation layers in place
**Target state:** ✅ Comprehensive regex patterns for all fields by Phase 2

---

### 8. OWASP Top 10 Mitigations

| Vulnerability | GS-Server Risk | Mitigation | Status |
|---|---|---|---|
| **A01: Broken Access Control** | HIGH | @PreAuthorize + tenant validation | ✅ Implemented |
| **A02: Cryptographic Failures** | HIGH | TLS 1.2+ (target), bcrypt passwords | 🟨 Partial |
| **A03: Injection** | MEDIUM | Input validation, no SQL (file-based) | ✅ Safe |
| **A04: Insecure Design** | MEDIUM | Security by design, this policy | ✅ Implemented |
| **A05: Security Misconfiguration** | MEDIUM | Secure defaults, no hardcoded secrets | ✅ Implemented |
| **A06: Vulnerable Components** | LOW | Dependency scanning in CI/CD | 📋 Planned |
| **A07: Authentication Failures** | MEDIUM | Bcrypt passwords, OAuth2 (target) | 🟨 Partial |
| **A08: Software/Data Integrity** | MEDIUM | Signed dependencies (Phase 6) | 📋 Planned |
| **A09: Logging/Monitoring Failures** | HIGH | Audit logging (Phase 1) | ❌ Missing |
| **A10: SSRF** | LOW | Not applicable (no outbound requests) | ✅ N/A |

---

## Role-Based Access Control (RBAC)

**4 role groups (target):**

| Role | Hardening | Proxy | Firewall | Users | Audit |
|---|---|---|---|---|---|
| **VIEWER** | Read | Read | Read | Read | Read |
| **OPERATOR** | Write | Write | Write | None | None |
| **ADMIN** | All | All | All | Manage | Read |
| **SUPERUSER** | All | All | All | All | All |

**Enforcement:**
```java
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public ResponseEntity<?> harden(HardeningRequest req) { ... }

@PreAuthorize("hasAuthority('GROUP_ADMIN')")
public ResponseEntity<?> createUser(UserRequest req) { ... }
```

---

## Pre-Deploy Security Checklist

- [ ] No secrets in code or config
- [ ] All endpoints protected by @PreAuthorize
- [ ] Cross-tenant denial tests passing
- [ ] Input validation working (400 or 422 on invalid)
- [ ] Secrets redacted from error messages
- [ ] HTTPS/TLS enforced for production
- [ ] Authentication mechanism verified
- [ ] Rate limiting configured and tested
- [ ] Audit logging implemented
- [ ] Correlation IDs in all responses
- [ ] Dependency vulnerabilities scanned
- [ ] Security headers configured (HSTS, CSP)
- [ ] Multi-tenant isolation verified
- [ ] Data encryption at rest configured
- [ ] Backup encryption verified

---

## Incident Response (Target)

### P1 Incident (Secret Exposure)

1. **Immediate (< 30 min):**
   - Revoke exposed secret
   - Isolate affected system
   - Notify security team

2. **Short-term (< 4 hours):**
   - Determine blast radius
   - Notify affected customers
   - Audit access logs

3. **Long-term:**
   - Root cause analysis
   - Implement prevention
   - Update policies

### P2 Incident (Unauthorized Access)

1. **Immediate:**
   - Block attacker
   - Check audit logs
   - Preserve evidence

2. **Follow-up:**
   - Determine what was accessed
   - Reset credentials if needed
   - Notify affected parties

---

## Summary

**GS-Server security target state:**
- ✅ Authentication + authorization (both layers)
- ✅ Multi-tenant isolation (verified by tests)
- ✅ Secrets redaction (Phase 1)
- ✅ Audit logging (Phase 1)
- ✅ Input validation (business rules)
- ✅ OWASP Top 10 mitigations
- ✅ RBAC (4 roles)
- ✅ Incident response plan

**Critical blocking gaps:**
1. Secrets redaction (Phase 1)
2. Audit logging (Phase 1)
3. Cross-tenant denial tests (Phase 2)

**Next:** See [[ROADMAP_TO_TARGET.md]] for phase-by-phase implementation.

