# GS-Server Target Architecture Principles

**Project:** GS-Server  
**Version:** 1.0 (Target State)  
**Last Updated:** 2026-07-24  
**Next Review:** 2026-10-24

---

## Overview

This document defines the target architectural principles for GS-Server when fully production-ready. It builds on the present-state architecture (78 components, 4-layer design) and applies organizational standards from `/mnt/DATA/WORKSPACE/project-settings/common` to establish a clear vision for gaps and evolution.

---

## Eight Core Principles

### 1. Unified Platform Abstraction

**One API for all platforms; execute uniformly across Linux, Windows, macOS, and cloud.**

**Why:**
- Server operators manage infrastructure the same way regardless of platform
- Reduces training and operational complexity
- Enables consistent automation across heterogeneous infrastructure
- Single integration point for management tools and dashboards

**Key characteristics:**
- ✅ Single API for hardening, proxy, firewall, deployments (all platforms)
- ✅ Platform-agnostic configuration (not Bash, not PowerShell, not platform CLIs)
- ✅ Full operation history with rollback on all platforms
- ✅ Audit trail uniform across platforms
- ❌ Platform-specific scripts exposed to operators
- ❌ Manual server tweaking without tracking

**Current state:** ✅ Implemented (LinuxAdapter, WindowsAdapter abstracted behind service)

**How to apply:**
- When adding capability: Create platform-agnostic service interface first
- Implement platform-specific adapters that conform to interface
- Hide adapter selection logic in service (auto-detect or config)
- Test parity across platforms (Windows config should behave identically to Linux)

**Exceptions:**
- Internal implementation can use platform-specific libraries (pty4j, ProcessBuilder)
- Configuration can mention platform to operators, but request validation rejects platform-specific options
- Example: Invalid to require "bash" in profile name; valid to support baseline and strict profiles on all platforms

---

### 2. Clean Separation of Concerns (Four-Tier Minimum, Six-Tier Target)

**Each tier has one responsibility; no tier crosses into another's domain.**

**Why:**
- Testability: Each tier testable independently
- Maintainability: Changes in one tier don't cascade
- Reusability: Services can be reused by different controllers
- Scaling: Each tier scales independently

**Present-state (4-tier):**
```
Tier 1: HTTP Controllers (@RestController)    [routing + format validation]
Tier 2: Services (@Service)                    [business logic + orchestration]
Tier 3: Adapters (@Component)                  [platform-specific execution]
Tier 4: Repositories (@Repository)             [persistence]
```

**Target-state (6-tier, organizational standard):**
```
Tier 1: UI/Frontend (Angular SPA)              [rendering + user interaction]
Tier 2: UI Orchestrators (Angular Services)    [state management + HTTP calls]
Tier 3: REST API Controllers (@RestController) [HTTP endpoints + format validation]
Tier 4: Business Services (@Service)           [business logic + authorization]
Tier 5: Repositories (@Repository)             [data persistence]
Tier 6: Database (SQLite/PostgreSQL)           [actual data store]
```

**Current state:** ⚠️ Partially implemented (4-tier exists, jar not wired for REST, no database)

**Layer responsibilities (MANDATORY):**

| Tier | Can Do | Cannot Do |
|---|---|---|
| **Controllers** | Route HTTP, validate format, check auth | Business logic, system ops |
| **Services** | Validate rules, orchestrate, audit | Direct execution, HTTP handling |
| **Adapters** | Execute ops (ProcessBuilder, file I/O) | Routing, validation |
| **Repositories** | Persist/query state | Business logic, ops |

**How to apply:**
- Handler/Controller: Only `return service.doWork(request)`
- Service: Only `validateRequest()`, then `adapter.execute()` or `repository.save()`
- Adapter: Only `ProcessBuilder.start()` or system calls, return result
- Repository: Only file/database read/write, return data objects

---

### 3. Multi-Tenant Isolation (Mandatory, Verified by Tests)

**Reject cross-tenant access BEFORE repository access. Scope all queries by tenant.**

**Why:**
- Compliance: Legal requirement for hosted services
- Security: Prevents data leaks between customers
- Audit: Operator actions scoped to their tenant
- Trust: Customers require proof of isolation

**Current state:** ⚠️ Partially implemented (validation in service, NOT tested for denial)

**Key characteristics:**
- ✅ Every operation includes tenantId
- ✅ Service validates tenant access before execution
- ✅ Repository queries scoped to tenant
- ✅ Audit logs include tenant context
- ❌ **CRITICAL:** No cross-tenant denial tests (authorization not verified)
- ❌ **CRITICAL:** No audit logging (who accessed what tenant?)

**How to apply:**
- Every API endpoint requires explicit `tenantId` parameter
- Service method: `validateTenantAccess(tenantId)` throws if invalid
- Repository method: `findByTenantId(tenantId)` (never unscoped queries)
- Test requirement: Write test_userFromTenantA_cannotAccessTenantB_data()

**Audit trail requirement:**
```json
{
  "timestamp": "2026-07-24T10:00:00Z",
  "actor": "operator@company.com",
  "tenantId": "tenant-a",
  "action": "hardening.apply",
  "result": "success",
  "details": { "profile": "strict", "platform": "linux" }
}
```

---

### 4. Technology Abstraction in Naming

**Don't mention tech in names unless it's the ONLY technology for that component.**

**Why:**
- Future flexibility: Can swap nginx for Apache without renaming every class
- Clear intent: Name describes business concern, not implementation detail
- Consistency: Naming follows organizational standards

**Current state:** ⚠️ Mixed (good: HardeningHandler; needs work: ProcessNginxCommandExecutor)

**Rules:**
- ❌ `ProxyServiceForNginx` (Nginx shouldn't be in name - other proxies exist)
- ✅ `GatewayProxyService` (abstracts proxy type)
- ❌ `LinuxHardeningAdapter` in service names (only in adapter classes)
- ✅ `HardeningService` (abstracts platform)
- ❌ HttpProxyController (HTTP shouldn't be in name)
- ✅ `GatewayProxyController` (abstracts protocol and proxy type)

**Exceptions:**
- ✅ Adapter classes CAN mention tech: `LinuxHardeningAdapter`, `ProcessNginxCommandExecutor`
- ✅ Executor classes CAN mention tech: `ProcessBuilder`, `pty4jTerminalSession`
- Rationale: Adapters and executors are platform/tech-specific by definition

**How to apply:**
- When naming: "Does this component work for ONLY this tech?" If no → don't mention tech
- Code review: Reject PRs with tech names in controllers/services
- Refactor: Phase 2 task to rename controllers to handlers

---

### 5. Fail-Safe Operations with Full History

**Every operation produces immutable history; any state is recoverable.**

**Why:**
- Disaster recovery: Rollback to any prior state in seconds
- Audit: Full history of who did what and when
- Debugging: See entire operation sequence when troubleshooting
- Compliance: Immutable audit trail for audits

**Current state:** ✅ Implemented (JSON files, full history, rollback tested)

**Key characteristics:**
- ✅ Every operation persisted with state
- ✅ Full history maintained (not just latest)
- ✅ Rollback available for any operation
- ✅ State immutable once saved (append-only, no updates)
- ❌ **CRITICAL:** No encryption at rest (plaintext JSON)
- ❌ No database backup encryption

**Implementation (present):**
```
Operation Flow:
1. Request received → create OperationState (PENDING)
2. Execute via adapter
3. Capture result (SUCCESS/FAILED)
4. Persist to disk (append-only JSON)
5. Return response
6. User can rollback to any prior operationId
```

**Target evolution:**
- Phase 1: Secrets redaction (no leaking credentials in error messages)
- Phase 2: Structured errors (errorId, code, details)
- Phase 3: Database persistence (SQLite, encrypted at rest)
- Phase 6: Compliance backup (encrypt backups, 1-year retention)

---

### 6. Security First (Cryptography, Audit, Secrets)

**Security is built-in, not added later. Every layer validates and enforces.**

**Why:**
- Breaches are expensive (legal, reputation, data loss)
- Compliance mandates (SOC2, GDPR, HIPAA if applicable)
- Trust: Customers only use secure infrastructure management
- Proactive: Prevent incidents instead of reacting

**Current state:** ⚠️ Partially implemented (passwords hashed, but audit logging missing)

**Security layers (defense-in-depth):**

| Layer | Responsibility | Present | Target |
|-------|---|---|---|
| **Application** | Input validation, authorization | ✅ | ✅ |
| **API** | Authentication, @PreAuthorize | ✅ | ✅ OAuth2 |
| **Transport** | TLS 1.2+ for HTTPS | ❌ Dev HTTP | ✅ Enforce |
| **Data** | Encryption at rest, audit logs | ❌ Plaintext JSON | ✅ SQLite + encrypted |
| **Infrastructure** | Access controls, network isolation | ⚠️ Partial | ✅ Complete |

**Critical rules (NON-NEGOTIABLE):**
- ✅ Validate authorization in BOTH API layer AND Service layer
- ✅ Encrypt passwords with bcrypt (not plaintext)
- ✅ Audit every write operation (who, what, when, why)
- ✅ Redact secrets from error messages and logs
- ✅ Use strong TLS (1.2+) for all production HTTPS
- ✅ Scope all queries by tenant (multi-tenant isolation)

**Secrets redaction (CRITICAL, Phase 1):**
```
❌ BAD error message: "Failed to connect: private_key at /root/.ssh/id_rsa not found"
✅ GOOD error message: "Failed to connect (check network configuration)"
    ^ Log contains full error internally, response to user is generic
```

---

### 7. Observable Operations (Structured Logs, Metrics, Traces)

**Every operation is visible: structured logs, metrics, and traces show what happened.**

**Why:**
- Debugging: Trace a slow request across services
- Monitoring: See operation success rate in real-time
- Alerting: Alert on high failure rates or latency spikes
- Compliance: Audit trail for regulatory reviews

**Current state:** ❌ Not implemented (no structured logging, no metrics, no tracing)

**Observability requirements (target):**

| Signal | Present | Target | Phase |
|---|---|---|---|
| Structured logs | ❌ | ✅ Tenant ID, correlation ID, actor | Phase 6 |
| Metrics | ❌ | ✅ Success rate, latency, error types | Phase 6 |
| Traces | ❌ | ✅ Request flow across services | Phase 6 |
| Audit trail | ❌ | ✅ Full operation history (1-year retention) | Phase 1 |
| Alerts | ❌ | ✅ Critical operation failures | Phase 6 |

**Structured log example:**
```json
{
  "timestamp": "2026-07-24T10:00:00.000Z",
  "correlationId": "req-uuid-12345",
  "tenantId": "tenant-a",
  "actor": "operator@company.com",
  "severity": "INFO",
  "action": "hardening.apply",
  "result": "success",
  "details": { "profile": "strict", "platform": "linux" }
}
```

---

### 8. Zero-Downtime Operations (Atomic Changes, Instant Rollback)

**Configuration changes don't interrupt service; rollback is instantaneous.**

**Why:**
- Availability: No customer impact from infrastructure changes
- Risk reduction: Rollback mitigates bad deployments
- SLA compliance: Uptime guarantees require zero-downtime ops

**Current state:** ⚠️ Partially implemented (history exists, atomic write not verified)

**Pattern (target):**
```
Backup current → Write new → Validate → Activate → On failure, restore backup
```

**Requirements:**
- ✅ No service interruption during updates
- ✅ Rollback in seconds, not minutes
- ✅ Failed changes revert automatically
- ❌ Atomic transaction not verified (Phase 2)
- ❌ Health check before activation (Phase 4)

---

## Architectural Constraints

### Operational Constraints
- Max operation execution time: 5 minutes (timeout + graceful shutdown)
- Max request size: 10 MB (prevent memory exhaustion)
- Tenant limit: 1000+ tenants per instance (scalable)
- User limit: 100,000+ users per system (with proper indexing)

### Data Constraints
- Operation history retention: 1+ year (immutable)
- Audit log retention: 1+ year (for compliance)
- Database size: Support millions of operations (SQL required by Phase 3)
- Backup encryption: AES-256 (Phase 6)

### Performance Targets (Unoptimized Now, Target by Phase 6)
- API response time: < 500 ms (95th percentile)
- Operation execution: Real (hardening 60-120 sec OK)
- Rollback time: < 10 seconds (from history)
- Login: < 200 ms
- Startup time: < 30 seconds

---

## Non-Functional Requirements (Target State)

| Requirement | Present | Target | Priority |
|---|---|---|---|
| **Availability** | Monolithic (5x9 impossible) | Scalable (4x9+) | P1 Phase 4 |
| **Performance** | Unoptimized | < 500ms latency | P2 Phase 6 |
| **Security** | Partial (no audit) | Comprehensive | P1 Phase 1 |
| **Reliability** | Decent (no rollback testing) | Proven (chaos testing) | P2 Phase 2 |
| **Maintainability** | Good (clean code) | Excellent (docs + examples) | P3 Ongoing |
| **Compliance** | None yet | SOC2 + GDPR ready | P1 Phase 6 |
| **Observability** | None | Full (logs, metrics, traces) | P2 Phase 6 |

---

## Technology Decisions (Target)

| Component | Present | Target | Rationale |
|---|---|---|---|
| **Backend** | Spring Boot 3.3.2 | Spring Boot 3.5+ | Latest LTS |
| **Persistence** | JSON files | SQLite + encrypted backups | Compliance, performance |
| **Database** | None | SQL (PostgreSQL option Phase 6+) | Scalability |
| **Authentication** | HTTP Basic | OAuth2 + JWT + MFA (Phase 6) | Enterprise standard |
| **Encryption** | None | AES-256 at rest, TLS 1.3 in transit | Compliance |
| **Observability** | None | ELK + Prometheus + Jaeger (Phase 6) | Standard stack |
| **Testing** | Unit + contract | Unit + integration + E2E + security | Coverage > 85% |

---

## Evolution Roadmap (Phased Approach)

**Timeline:** 28 weeks (6-7 months) to full production readiness

### Phase 1: Foundation (2-3 weeks, Week 1-3)
**Goal:** Production-ready hardening with audit trail

**Must-have:**
- Secrets redaction (credentials don't leak in errors)
- Audit logging framework (who/what/when recorded)
- Structured error model (errorId, code, details)

### Phase 2: Gateway Wiring (3-4 weeks, Week 4-8)
**Goal:** Multi-service architecture via REST

**Must-have:**
- Jar backend wired (UI calls via HTTP)
- Correlation IDs (trace across services)
- Cross-tenant denial tests (authorization verified)

### Phase 3: Firewall Control (6-8 weeks, Week 9-17)
**Goal:** Multi-platform firewall automation

### Phase 4: Application Management (8-10 weeks, Week 18-28)
**Goal:** Zero-downtime deployments

### Phase 5: Resource Management (6-8 weeks, ongoing)
**Goal:** CPU/memory quotas and auto-scaling

### Phase 6: Observability (10-12 weeks, ongoing)
**Goal:** Full audit, metrics, tracing, compliance

---

## Summary

**GS-Server target architecture:**
- **8 core principles** (unified abstraction, clean separation, multi-tenant isolation, etc.)
- **4-tier minimum, 6-tier target** architectural layers
- **Production-ready** security (audit, secrets redaction, encryption)
- **Fail-safe** with full history and instant rollback
- **Observable** via structured logging, metrics, and traces
- **Scalable** through independent tier scaling

**Gaps from present → target:**
- Phase 1: Secrets + audit logging (2-3 weeks)
- Phase 2: Jar wiring + structured errors + correlation IDs (3-4 weeks)
- Phase 3-6: Remaining capabilities and observability (20+ weeks)

**Next steps:** See [[ROADMAP_TO_TARGET.md]] for phase-by-phase implementation plan.

