# GS-Server Target Development Rules

**Project:** GS-Server  
**Version:** 1.0 (Target State)  
**Last Updated:** 2026-07-24  
**Based on:** `/mnt/DATA/WORKSPACE/project-settings/common/originals/development-rules/`

---

## Core Architectural Rules (MANDATORY)

### Rule 1: Three-Tier Separation

**Handler → Service → Adapter → Repository (four minimum, six target)**

**Tier responsibilities:**

| Tier | What ✅ | What ❌ |
|---|---|---|
| **Handler (@Controller)** | Route HTTP, validate format, @PreAuthorize | Business logic, system ops |
| **Service (@Service)** | Validate rules, orchestrate adapters/repos | HTTP handling, direct execution |
| **Adapter** | Execute operations (ProcessBuilder, file I/O) | Routing, validation, business logic |
| **Repository** | Persist/retrieve state | Business logic, operations |

**Implementation - CORRECT:**
```java
@Controller
public class HardeningHandler {
  @PostMapping
  @PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    return ResponseEntity.ok(service.harden(req));  // ← Only delegate
  }
}

@Service
public class DefaultHardeningService {
  public HardeningResult harden(HardeningRequest req) {
    validateRequest(req);              // ← Validate rules
    HardeningResult result = adapter.execute(req);  // ← Delegate
    repository.save(result);           // ← Persist
    return result;
  }
}
```

---

### Rule 2: Handlers Are Thin Orchestrators

**If handler contains more than delegation, move to Service.**

**❌ BAD:**
```java
@PostMapping("/harden")
public ResponseEntity<?> harden(HardeningRequest req) {
  if (req.profile().equals("strict")) {  // ← Validation in handler
    // ... more logic ...
  }
  return ResponseEntity.ok(...);
}
```

**✅ GOOD:**
```java
@PostMapping("/harden")
public ResponseEntity<?> harden(HardeningRequest req) {
  return ResponseEntity.ok(service.harden(req));  // ← Just delegate
}
```

---

### Rule 3: Services Are Thin Orchestrators

**If service contains more than validation + delegation, move to Adapter.**

**❌ BAD:**
```java
@Service
public class HardeningService {
  public void harden() {
    ProcessBuilder pb = new ProcessBuilder("sysctl", "-w", ...);  // ← Execution in service
    pb.start();
  }
}
```

**✅ GOOD:**
```java
@Service
public class HardeningService {
  public void harden() {
    validateRequest(req);        // ← Validate rules
    adapter.execute(req);        // ← Delegate
    repository.save(result);     // ← Persist
  }
}
```

---

### Rule 4: Dependency Injection, Not Instantiation

**Inject dependencies, don't `new` them in constructors.**

**❌ BAD:**
```java
@Service
public class HardeningService {
  private LinuxAdapter adapter = new LinuxHardeningAdapter();  // ← Hard to test
}
```

**✅ GOOD:**
```java
@Service
public class HardeningService {
  private final HardeningAdapter adapter;
  
  public HardeningService(HardeningAdapter adapter) {
    this.adapter = adapter;  // ← Injected
  }
}
```

---

### Rule 5: Authorization in Both API and Service Layers

**Never trust UI-only authorization. Validate at every layer.**

**Pattern - MANDATORY:**
```java
// Handler validates authentication + authorization
@Controller
@PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
public class HardeningHandler {
  @PostMapping("/harden")
  public ResponseEntity<?> harden(@RequestBody HardeningRequest req) {
    return ResponseEntity.ok(service.harden(req));
  }
}

// Service validates business authorization (can't trust handler)
@Service
public class DefaultHardeningService {
  public HardeningResult harden(HardeningRequest req) {
    validateTenantAccess(req.tenantId());        // ← Validate again
    validateOperatorAccess(req.requestedBy());   // ← Validate again
    return adapter.execute(req);
  }
}
```

---

## Naming Conventions (MANDATORY)

### Classes

**Pattern:** `[Domain][Component][Suffix]`

| Component | Suffix | Example |
|---|---|---|
| HTTP Controller (infrastructure) | `Handler` | `HardeningHandler` |
| HTTP Controller (regular) | `Controller` | `UserController` |
| Service | `Service` | `DefaultHardeningService` |
| Adapter | `Adapter` | `LinuxHardeningAdapter` |
| Executor (tech-specific) | `Executor` or `CommandExecutor` | `ProcessNginxCommandExecutor` |
| Repository | `Repository` | `FileBasedHardeningOperationStateRepository` |
| Test | `Test` | `HardeningServiceTest` |

**Rules:**
- ✅ `GatewayProxyService` (abstracts proxy type - good)
- ✅ `HardeningHandler` (abstracts platform - good)
- ✅ `LinuxHardeningAdapter` (platform-specific adapter - OK)
- ✅ `ProcessNginxCommandExecutor` (tech-specific executor - OK)
- ❌ `ProxyServiceForNginx` (don't mention tech in service - bad)
- ❌ `HttpProxyController` (don't mention protocol in controller - bad)

---

### Methods

**Pattern:** Start with action verb

**✅ GOOD - describes what it does:**
```
harden()
validateRequest()
buildCommand()
executeOperation()
persistState()
```

**❌ BAD - unclear:**
```
process()
handle()
doWork()
something()
```

---

### Variables

**Pattern:** Descriptive nouns, no Hungarian notation

**✅ GOOD:**
```
HardeningOperationState state
String tenantId
int upstreamPort
```

**❌ BAD:**
```
HardeningOperationState s
String strTenantId (Hungarian)
int iPort
```

---

## Code Review Checklist (PRE-MERGE)

### Architecture

- [ ] Request flows through Handler → Service → Adapter → Repository?
- [ ] No business logic in Handler?
- [ ] No direct operations (ProcessBuilder, file I/O) in Service?
- [ ] All platform-specific logic in Adapter?
- [ ] No technology names in controllers/services (only adapters)?
- [ ] All dependencies injected (no `new` operator)?
- [ ] No hardcoded dependencies?

### Authorization & Security

- [ ] Handler has @PreAuthorize?
- [ ] Service validates tenant/operator (never trust handler)?
- [ ] Cross-tenant access prevented?
- [ ] No secrets in error messages?
- [ ] No secrets in logs?
- [ ] Input validated before execution?

### Testing

- [ ] Unit tests for business logic?
- [ ] Integration tests for service + adapter?
- [ ] Cross-tenant denial test included?
- [ ] No production data in tests?
- [ ] Tests are deterministic?

### Quality

- [ ] No hardcoded values (magic numbers)?
- [ ] Error handling present?
- [ ] Logging/auditing captured?
- [ ] No TODOs without issue number?
- [ ] Naming follows conventions?

---

## Violations & How to Spot Them

### Violation 1: Logic in Handler

**Symptom:** Handler does more than delegate

**Fix:** Move to Service

---

### Violation 2: Direct System Calls in Service

**Symptom:** ProcessBuilder or file ops in service

**Fix:** Move to Adapter

---

### Violation 3: Hardcoded Dependencies

**Symptom:** `new` operator instead of injection

**Fix:** Inject via constructor

---

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

---

## Common Mistakes & Prevention

| Mistake | Prevention | Status |
|---|---|---|
| Business logic in handler | Code review checklist | ✅ |
| System calls in service | Code review checklist | ✅ |
| Hardcoded dependencies | Dependency injection | ✅ |
| Unvalidated operations | Security review | ✅ |
| Cross-tenant access | Cross-tenant tests | ❌ Missing |
| Secrets in logs | Redaction review | ❌ Missing |
| No rollback | Adapter pattern required | ✅ |
| Tight coupling | Dependency injection | ✅ |

---

## Development Workflow (RED-GREEN-REFACTOR)

**For all code changes (features, fixes, refactors):**

1. **RED:** Write failing test defining expected behavior
2. **GREEN:** Write minimal code to make test pass
3. **REFACTOR:** Improve code while keeping tests passing

**For bug fixes:**
1. Write test that reproduces bug (FAILS with current code)
2. Fix the bug (test PASSES)
3. Commit both together (test + fix)

---

## Summary

**Target development rules:**
- ✅ Four-tier minimum separation (Handler → Service → Adapter → Repository)
- ✅ Thin orchestrators (handlers, services delegate)
- ✅ Dependency injection (no hardcoded `new`)
- ✅ Authorization in both layers (API + Service)
- ✅ Naming conventions (no tech in service names)
- ✅ Code review checklist (architecture, security, testing)
- ✅ RED-GREEN-REFACTOR (test-first development)

**Current issues:** Mixed naming, jar not wired
**Phase 2 fixes:** Rename controllers to handlers, wire jar backend

