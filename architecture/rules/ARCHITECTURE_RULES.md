# GS-Server Architecture Rules

**Extracted from:** TARGET_ARCHITECTURE_PRINCIPLES.md, TARGET_DEVELOPMENT_RULES.md  
**Last Updated:** 2026-07-24

---

## Core Principles

### Rule 1: Unified Server Management Without Technical Complexity

Define server operations once, execute identically across all platforms (Linux, Windows, macOS, cloud).

**Key characteristics:**
- ✅ Single API for hardening, proxy, firewall, deployments across all platforms
- ✅ Platform-agnostic configuration (not Bash, not PowerShell, not CLI)
- ✅ Full operation history with rollback capability
- ✅ Audit trail of who did what, when, why
- ❌ Platform-specific scripts in config files
- ❌ Manual server tweaking without tracking

---

### Rule 2: Clean Separation of Concerns

Each architectural layer has one responsibility; no layer executes another's job.

**Layer responsibilities:**

| Layer | What It Does | What It CANNOT Do |
|-------|---|---|
| **Handler (@Controller)** | Route HTTP, validate format | Business logic, system ops |
| **Service (@Service)** | Validate rules, orchestrate | Direct execution, HTTP |
| **Adapter** | Execute system commands | Routing, validation, orchestration |
| **Repository (@Repository)** | Persist/retrieve state | Business logic, ops |

**How to apply:**
- Handlers: Only `return service.doWork(request)`
- Services: Only `return adapter.execute(command)` or `repository.save(state)`
- Adapters: Only `ProcessBuilder.start()` or direct OS APIs
- Repositories: Only file/database read/write

---

### Rule 3: Platform Abstraction Through Adapters

Hide platform differences behind uniform service interfaces.

**Pattern:**
```
Service (handles policy/validation)
  ↓ delegates to
Adapter (platform-specific execution)
  ├─ LinuxAdapter
  ├─ WindowsAdapter
  ├─ MacAdapter
  └─ CloudAdapter
  ↓ returns result
Service (captures state, audits result)
```

**Implementation:**
- Each platform gets its own adapter
- Service delegates to adapter without knowing platform details
- Adapters can mention specific technologies (they're platform-specific)

---

### Rule 4: Technology Abstraction in Naming

Don't mention specific technologies in handler/service names unless it's the ONLY technology.

**Examples:**

| Name | Tech Mentioned? | Reason |
|---|---|---|
| `GatewayProxyHandler` | ❌ No | Works with nginx, Apache, Envoy |
| `NginxConfigurationHandler` | ✅ Yes | ONLY works for nginx |
| `HardeningHandler` | ❌ No | Works across platforms |
| `LinuxHardeningAdapter` | ✅ Yes | Platform-specific (expected) |

---

### Rule 5: Fail-Safe Operations With Rollback

Every operation produces immutable history; any state is recoverable.

**Implementation:**
```
Operation Flow:
1. Request received → create OperationState (PENDING)
2. Execute via adapter
3. Capture result (SUCCESS/FAILED)
4. Persist to disk (append-only history)
5. Return response
6. User can rollback to any previous state
```

**Requirements:**
- Every operation persisted with state
- Full history maintained (not just latest)
- Rollback always available
- State immutable once saved

---

### Rule 6: Security First

Security is built in, not added later. Every layer validates and enforces.

**Security layers:**

| Layer | Responsibility |
|-------|---|
| **Application** | Spring Security + @PreAuthorize |
| **API** | Authentication + authorization checks |
| **Transport** | TLS 1.2+ for HTTPS |
| **Data** | Encryption at rest, audit logging |
| **Infrastructure** | Access controls, isolation |

**Critical rule:** Validate authorization in BOTH API layer AND Service layer. Never trust UI-only authorization.

---

### Rule 7: Observable Operations

Every operation is visible: structured logs, metrics, and traces show what happened.

**Observability requirements:**
- Structured logs (tenant ID, correlation ID, actor, severity)
- Metrics (operation success rate, latency, error types)
- Traces (request flow across services)
- Audit trail (full operation history with actor)
- Alerts (critical operation failures)

---

### Rule 8: Zero-Downtime Operations

Configuration changes don't interrupt service; rollback is instantaneous.

**Pattern:**
```
Backup current → Write new → Validate → Activate → On failure, restore backup
```

**Requirements:**
- No service interruption during updates
- Rollback in seconds, not minutes
- Failed changes revert automatically

---

## Three-Tier Separation Rules

### Rule 1: Handler Is Thin Orchestrator

**If a handler contains more than delegation, move it to Service.**

**❌ BAD:**
```java
@Controller
public class HardeningController {
  @PostMapping("/harden")
  public ResponseEntity<?> harden(HardeningRequest req) {
    if (req.profile().equals("strict")) {
      // ← Validation logic in handler
    }
  }
}
```

**✅ GOOD:**
```java
@Controller
public class HardeningHandler {
  @PostMapping("/harden")
  public ResponseEntity<?> harden(HardeningRequest req) {
    return ResponseEntity.ok(service.harden(req));  // Only delegate
  }
}
```

---

### Rule 2: Service Is Thin Orchestrator

**If a service contains more than validation and delegation, move it to Adapter.**

**❌ BAD:**
```java
@Service
public class HardeningService {
  public void harden() {
    ProcessBuilder pb = new ProcessBuilder(...);  // ← System call in service
    pb.start();
  }
}
```

**✅ GOOD:**
```java
@Service
public class HardeningService {
  public void harden() {
    validateRequest(req);        // Business rules
    adapter.execute(req);        // Delegate execution
    repository.save(result);     // Persist
  }
}
```

---

### Rule 3: Dependency Injection, Not Instantiation

**Inject dependencies, don't `new` them in constructors.**

**❌ BAD:**
```java
@Service
public class HardeningService {
  private HardeningAdapter adapter = new LinuxHardeningAdapter();  // Hard to test
}
```

**✅ GOOD:**
```java
@Service
public class HardeningService {
  private final HardeningAdapter adapter;
  
  public HardeningService(HardeningAdapter adapter) {  // Injected
    this.adapter = adapter;
  }
}
```

---

### Rule 4: Authorization in Both API and Service Layers

**Never trust UI-only authorization. Validate at every layer.**

**Pattern:**
```java
// Handler validates format and authorization
@Controller
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public class HardeningHandler {
  @PostMapping("/harden")
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    return ResponseEntity.ok(service.harden(req));
  }
}

// Service validates business rules again
@Service
public class DefaultHardeningService {
  public HardeningResult harden(HardeningRequest req) {
    validateTenantAccess(req.tenantId());        // Validate again
    validateOperatorAccess(req.requestedBy());   // Validate again
    return adapter.execute(req);
  }
}
```

---

## Violation Detection

### Violation 1: Logic in Handler

**Symptom:** Handler does more than delegate

**Fix:** Move to Service

### Violation 2: Direct System Calls in Service

**Symptom:** ProcessBuilder or file ops in service

**Fix:** Move to Adapter

### Violation 3: Hardcoded Dependencies

**Symptom:** `new` operator instead of injection

**Fix:** Inject via constructor

### Violation 4: Unvalidated Operations

**Symptom:** Service executes without auth check

**Fix:** Validate tenant/operator before execution

---

## When to Refactor

**✅ DO refactor when:**
- Code duplication across adapters (DRY)
- Layer violations (logic in wrong tier)
- Naming confusion
- Tests get too complex

**❌ DON'T refactor for:**
- "Looks bad" (cosmetic)
- Hypothetical improvements
- Premature optimization

