# GS-Server Layer Architecture (Present)

**Project:** GS-Server  
**Version:** 1.0 (Present State)  
**Last Updated:** 2026-07-24

---

## Four-Layer Architecture Pattern

GS-Server implements a four-tier separation of concerns:

```
┌─────────────────────────────────────┐
│ TIER 1: HTTP/REST Controllers       │
│ @RestController, @PreAuthorize      │
│ Routing + format validation         │
└─────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│ TIER 2: Services                    │
│ @Service, business logic            │
│ Validation + orchestration          │
└─────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│ TIER 3: Adapters                    │
│ @Component, platform-specific       │
│ Real execution (ProcessBuilder)     │
└─────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│ TIER 4: Repositories                │
│ @Repository, persistence            │
│ File I/O, JSON serialization        │
└─────────────────────────────────────┘
```

---

## Tier 1: HTTP REST Controllers

**Responsibility:** Route HTTP requests, validate format, delegate to services

**Spring annotation:** `@RestController`

**Examples:**
```java
@RestController
@RequestMapping("/api/v1/hardening")
public class HardeningController {
  @PostMapping
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS')")
  public ResponseEntity<HardeningResponse> triggerHardening(
      @RequestBody HardeningRequest request) {
    // ✅ Validate format (JSON structure, types) → handled by @RequestBody
    // ✅ Check authorization → handled by @PreAuthorize
    // ✅ Delegate to service → only responsibility
    HardeningResponse response = hardeningService.triggerHardening(request);
    return ResponseEntity.accepted().body(response);  // 202 Accepted
  }
}
```

**What controllers DO:**
- ✅ Route HTTP requests via @RequestMapping, @PostMapping, @GetMapping
- ✅ Validate request format (@RequestBody deserializes JSON)
- ✅ Check authorization (@PreAuthorize checks Spring Security)
- ✅ Parse path parameters, query strings
- ✅ Call service and return response
- ✅ Return appropriate HTTP status codes (202, 404, 422, etc.)

**What controllers DON'T:**
- ❌ Business logic (validation rules, constraints)
- ❌ System operations (ProcessBuilder, file I/O)
- ❌ Data access (directly calling repositories)

**HTTP Status Codes Used:**
- 202 Accepted (asynchronous operation started)
- 200 OK (synchronous success, query operations)
- 400 Bad Request (malformed JSON - Spring handles)
- 401 Unauthorized (not authenticated)
- 403 Forbidden (@PreAuthorize denied)
- 404 Not Found (resource missing)
- 500 Internal Server Error (unhandled exception)

**Current Controllers (8 total):**
1. HardeningController
2. GatewayProxyController
3. ProxyController
4. ProxyInstallationController
5. UserManagementController
6. AuthController
7. TerminalTicketController
8. SpaForwardController (special: SPA routing)

---

## Tier 2: Services

**Responsibility:** Validate business rules, orchestrate adapters and repositories, handle errors

**Spring annotation:** `@Service`

**Examples:**
```java
@Service
public class DefaultHardeningService implements HardeningService {
  private final LinuxHardeningAdapter linuxAdapter;
  private final WindowsHardeningAdapter windowsAdapter;
  private final HardeningOperationStateStore operationStateStore;

  @Override
  public HardeningResponse triggerHardening(HardeningRequest request) {
    // ✅ VALIDATE: business rules
    validateRequest(request);  // profile, tenant, operator exist?
    validateTenantAccess(request.tenantId());  // user authorized for this tenant?
    
    // ✅ ORCHESTRATE: call adapters
    String operationId = UUID.randomUUID().toString();
    HardeningExecutionReport report = executeByProfile(request.profile());
    
    // ✅ HANDLE FAILURE: rollback if needed
    if (!report.successful()) {
      HardeningExecutionReport rollbackReport = rollbackByProfile(request.profile());
      // ... persist failed state
      throw new HardeningExecutionException(...);
    }
    
    // ✅ PERSIST: save state via repository
    operationStateStore.save(new HardeningOperationState(
        operationId, occurredAtUtc, "success",
        request.tenantId(), request.requestedBy(),
        request.profile(), report.platform(), ...));
    
    // ✅ RETURN: domain object to controller
    return new HardeningResponse("accepted", "Hardening started");
  }

  // ✅ VALIDATE: business constraints
  private void validateRequest(HardeningRequest request) {
    if (!ALLOWED_TENANTS.contains(request.tenantId())) {
      throw new PolicyViolationException("Invalid tenant");
    }
    if (!ALLOWED_OPERATORS.contains(request.requestedBy())) {
      throw new PolicyViolationException("Invalid operator");
    }
    if (!ALLOWED_PROFILES.contains(request.profile())) {
      throw new PolicyViolationException("Invalid profile");
    }
  }
}
```

**What services DO:**
- ✅ Validate business rules (tenant exists, profile valid, operator authorized)
- ✅ Validate authorization (tenant access control)
- ✅ Orchestrate adapters (call execute, handle success/failure)
- ✅ Persist state via repositories
- ✅ Build domain objects for responses
- ✅ Handle errors and decide rollback strategy

**What services DON'T:**
- ❌ Handle HTTP (routing, status codes)
- ❌ Direct system execution (ProcessBuilder directly)
- ❌ Direct file I/O (always via repository)

**Validation Layer Separation:**
```
HardeningController
  ├─ Validates FORMAT (Spring @RequestBody)
  │   └─ JSON structure, types, required fields
  │
  └─ HardeningService.triggerHardening()
      ├─ Validates RULES (business constraints)
      │   └─ Tenant valid? Operator valid? Profile valid?
      │
      ├─ Validates AUTHORIZATION (multi-tenant isolation)
      │   └─ Does requestedBy have access to tenantId?
      │
      └─ Calls adapter
```

**Current Services (8 total):**
1. DefaultHardeningService (interface: HardeningService)
2. DefaultGatewayProxyService (interface: GatewayProxyService)
3. DefaultProxyService (interface: ProxyService)
4. ProxyInstallationService (concrete only)
5. UserManagementService (concrete only)
6. TerminalSessionManager (concrete only)
7. JsonUserDetailsService (implements Spring UserDetailsService)
8. JsonUserStore (concrete only)

---

## Tier 3: Adapters

**Responsibility:** Platform-specific or technology-specific execution

**Spring annotation:** `@Component`

**Examples:**
```java
@Component
public class LinuxHardeningAdapter {
  private final HardeningCommandExecutor commandExecutor;

  public HardeningExecutionReport applyBaselineHardening() {
    // ✅ TECHNOLOGY-SPECIFIC: bash script with platform details
    String script = """
        set -e
        echo "=== Linux Baseline Hardening ==="
        apt-get update -qq
        ufw --force enable
        # ... more Linux-specific commands
        """;

    // ✅ EXECUTE: ProcessBuilder (real system call)
    List<String> command = List.of("/usr/bin/env", "bash", "-lc", script);
    CommandExecutionResult result = commandExecutor.execute(
        command, Duration.ofMinutes(2));
    
    // ✅ RETURN: execution report to service
    return new HardeningExecutionReport(
        "linux",                    // platform
        result.exitCode(),          // success indicator
        result.stdout(),            // command output
        result.stderr(),            // error output
        result.timedOut());         // timeout flag
  }
}
```

**ProcessBuilder Execution Pattern:**
```java
// In ProcessHardeningCommandExecutor:
public CommandExecutionResult execute(List<String> command, Duration timeout) {
  try {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    Process process = pb.start();
    
    // Capture output
    String stdout = readStream(process.getInputStream());
    String stderr = readStream(process.getErrorStream());
    
    // Wait with timeout
    boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!completed) {
      process.destroyForcibly();
      return new CommandExecutionResult(124, "", "", true);  // timeout exit code
    }
    
    return new CommandExecutionResult(
        process.exitValue(), stdout, stderr, false);
  } catch (Exception e) {
    throw new RuntimeException("Execution failed", e);
  }
}
```

**What adapters DO:**
- ✅ Platform-specific implementations (Linux vs Windows)
- ✅ Real system execution (ProcessBuilder, file I/O)
- ✅ Technology-specific command building (bash vs PowerShell)
- ✅ Timeout handling
- ✅ Output parsing and reporting

**What adapters DON'T:**
- ❌ Validation (service validates before calling)
- ❌ HTTP handling
- ❌ Data persistence

**Adapter Pattern (Abstract Behind Interface):**
```
HardeningService
    ├─ Needs "execute hardening"
    └─ Doesn't care if Linux or Windows
        ↓
    └─ Calls adapter (selected by platform detection)
        ├─ LinuxHardeningAdapter
        │   └─ Bash scripts, Linux commands
        │
        └─ WindowsHardeningAdapter
            └─ PowerShell scripts, Windows commands
```

**Current Adapters:**
1. LinuxHardeningAdapter (platform-specific)
2. WindowsHardeningAdapter (platform-specific)
3. ProcessHardeningCommandExecutor (technology: ProcessBuilder)
4. ProcessNginxCommandExecutor (technology: ProcessBuilder + nginx)
5. InteractiveTerminalSession (technology: pty4j)

---

## Tier 4: Repositories

**Responsibility:** Persist and retrieve state (file I/O, JSON serialization)

**Spring annotation:** `@Repository`

**Examples:**
```java
public abstract class FileBasedOperationStateRepository<T> 
    implements OperationStateRepository<T> {
  
  private final ObjectMapper objectMapper;    // Jackson JSON
  private final Path storageDir;              // data/capability/
  
  @Override
  public void save(T state) {
    try {
      // ✅ SERIALIZE: domain object to JSON
      String fileName = getStateFileName(state);  // operation-{id}.json
      Path filePath = storageDir.resolve(fileName);
      String json = objectMapper.writeValueAsString(state);
      
      // ✅ PERSIST: write to filesystem
      Files.writeString(filePath, json,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
          
      logger.debug("Saved operation state: {}", fileName);
    } catch (IOException e) {
      logger.error("Failed to save operation state", e);
      throw new RuntimeException("Cannot save operation state", e);
    }
  }
  
  @Override
  public Optional<T> getLatest() {
    // ✅ LOAD: read latest state file from disk
    // Implementation scans directory, finds newest file
  }
}

// Concrete implementation:
public class FileBasedHardeningOperationStateRepository 
    extends FileBasedOperationStateRepository<HardeningOperationState> {
  
  @Override
  protected String getStateFileName(HardeningOperationState state) {
    return "operation-" + state.operationId() + ".json";
  }
}
```

**Storage Structure:**
```
data/
├── hardening/
│   ├── operation-550e8400-e29b-41d4-a716-446655440000.json
│   ├── operation-6ba7b810-9dad-11d1-80b4-00c04fd430c8.json
│   └── latest.json (copy of newest)
│
├── gateway-proxy/
│   ├── operation-6ba7b811-9dad-11d1-80b4-00c04fd430c8.json
│   └── latest.json
│
├── proxy/
│   ├── operation-6ba7b812-9dad-11d1-80b4-00c04fd430c8.json
│   └── latest.json
│
└── users/
    └── users.json (user list with bcrypt passwords)
```

**What repositories DO:**
- ✅ Serialize domain objects to JSON (Jackson ObjectMapper)
- ✅ Deserialize JSON back to domain objects
- ✅ Write files to filesystem
- ✅ Read files from filesystem
- ✅ Manage storage directories

**What repositories DON'T:**
- ❌ Business logic
- ❌ Validation
- ❌ HTTP handling

**Current Repositories:**
1. FileBasedHardeningOperationStateRepository
2. FileBasedProxyOperationStateRepository
3. FileBasedGatewayProxyOperationStateRepository
4. JsonUserStore (user persistence)

---

## State Store Pattern (Memory + Delegation)

**Pattern:** In-memory cache that delegates persistence to repository

```java
@Component
public class InMemoryHardeningOperationStateStore 
    implements HardeningOperationStateStore {
  
  private final AtomicReference<HardeningOperationState> latest;
  private final FileBasedHardeningOperationStateRepository repository;
  
  public InMemoryHardeningOperationStateStore(
      FileBasedHardeningOperationStateRepository repository) {
    this.repository = repository;
    this.latest.set(repository.getLatest().orElse(null));  // Load on startup
  }
  
  @Override
  public void save(HardeningOperationState state) {
    // ✅ Update memory
    latest.set(state);
    
    // ✅ Persist to disk (delegate)
    if (repository != null) {
      repository.save(state);
    }
  }
  
  @Override
  public Optional<HardeningOperationState> getLatest() {
    // ✅ Fast memory access
    return Optional.ofNullable(latest.get());
  }
}
```

**Why this pattern:**
- Fast queries (memory, not filesystem)
- Full history on disk (rollback capability)
- Graceful degradation (works without repository for testing)

---

## Data Flow Example: Hardening Request

```
1. HTTP REQUEST
   ├─ POST /api/v1/hardening
   ├─ Authorization: Basic username:password
   └─ Body: {"tenantId": "tenant-a", "profile": "strict", ...}

2. TIER 1: CONTROLLER
   ├─ HardeningController.triggerHardening()
   ├─ @PreAuthorize validates authentication
   ├─ @RequestBody deserializes JSON
   ├─ Calls hardeningService.triggerHardening(request)
   └─ Returns 202 Accepted

3. TIER 2: SERVICE
   ├─ DefaultHardeningService.triggerHardening()
   ├─ validateRequest(request)                    [Check profile valid]
   ├─ validateTenantAccess(request.tenantId())   [Check tenant exists]
   ├─ Detect platform (Linux or Windows)
   └─ Call adapter based on platform

4. TIER 3: ADAPTER
   ├─ LinuxHardeningAdapter.applyStrictHardening()
   ├─ Build bash script with Linux-specific commands
   ├─ Create ProcessBuilder with script
   ├─ CommandExecutor.execute(command, 2-min timeout)
   ├─ Capture stdout, stderr, exitCode
   └─ Return HardeningExecutionReport

5. SERVICE CONTINUES
   ├─ Check report.successful()
   ├─ If failed: rollbackByProfile() and persist failed state
   ├─ Persist HardeningOperationState to store
   └─ Build response DTO

6. CONTROLLER RETURNS
   ├─ ResponseEntity.accepted().body(response)
   └─ HTTP 202 Accepted

7. TIER 4: REPOSITORY (async)
   ├─ InMemoryHardeningOperationStateStore.save(state)
   ├─ Update in-memory latest
   ├─ Delegate to FileBasedHardeningOperationStateRepository
   ├─ ObjectMapper.writeValueAsString(state)
   ├─ Files.writeString(data/hardening/operation-{id}.json)
   └─ File persisted, ready for rollback
```

---

## Violation Detection

### Violation 1: Logic in Controller

**❌ BAD:**
```java
@RestController
public class HardeningController {
  @PostMapping
  public ResponseEntity<?> harden(HardeningRequest request) {
    if (request.profile() == null) {  // ← Business logic in controller
      throw new PolicyViolationException("Invalid profile");
    }
    return ResponseEntity.ok(service.harden(request));
  }
}
```

**✅ CORRECT:**
```java
@RestController
public class HardeningController {
  @PostMapping
  public ResponseEntity<?> harden(HardeningRequest request) {
    return ResponseEntity.ok(service.harden(request));  // ← Delegate only
  }
}
```

**Detection:** Look for if/switch/for/business logic in @RestController methods. Move to service.

---

### Violation 2: Direct System Calls in Service

**❌ BAD:**
```java
@Service
public class HardeningService {
  public void harden() {
    ProcessBuilder pb = new ProcessBuilder("sysctl", "-w", ...);  // ← System call
    pb.start();
  }
}
```

**✅ CORRECT:**
```java
@Service
public class HardeningService {
  private final HardeningAdapter adapter;
  
  public void harden() {
    adapter.execute(...);  // ← Delegate to adapter
  }
}
```

**Detection:** Look for ProcessBuilder, Runtime.getRuntime(), file I/O in @Service methods. Move to adapter.

---

### Violation 3: Hardcoded Dependencies

**❌ BAD:**
```java
@Service
public class HardeningService {
  private HardeningAdapter adapter = new LinuxHardeningAdapter();  // ← Hard to test
}
```

**✅ CORRECT:**
```java
@Service
public class HardeningService {
  private final HardeningAdapter adapter;
  
  public HardeningService(HardeningAdapter adapter) {  // ← Injected
    this.adapter = adapter;
  }
}
```

**Detection:** Look for `new` operator in class fields. Use constructor injection instead.

---

### Violation 4: Unvalidated Operations

**❌ BAD:**
```java
@Service
public class HardeningService {
  public void harden(HardeningRequest request) {
    return adapter.execute(request);  // ← No validation!
  }
}
```

**✅ CORRECT:**
```java
@Service
public class HardeningService {
  public void harden(HardeningRequest request) {
    validateRequest(request);              // ← Validate tenant
    validateTenantAccess(request.tenantId());
    return adapter.execute(request);       // ← Then execute
  }
}
```

**Detection:** Look for service methods that call adapters without validation. Add validation before execution.

---

## Summary

**Four-layer architecture in practice:**
1. **Controllers:** HTTP routing + format validation (thin)
2. **Services:** Business rules + orchestration (moderate)
3. **Adapters:** Platform-specific execution (real ProcessBuilder)
4. **Repositories:** File I/O + JSON serialization (dumb storage)

**Current implementation status:**
- ✅ Clean separation (violations are rare)
- ✅ Dependency injection (not hardcoded)
- ✅ Thin controllers (just delegate)
- ⚠️ Some service complexity (could be simplified)
- ✅ Real execution (actual hardening/proxy commands)
- ✅ Full history (append-only JSON, rollback support)

