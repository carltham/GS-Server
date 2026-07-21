# Dream Architecture Structured - Integrated with Common Standards

**The perfect server control framework, built on proven organizational standards**

---

## Executive Summary

This architecture marries our **Dream Architecture** with **Common Standards** from the organizational playbook:

- ✅ **Layered Architecture** (six-tier separation, mandatory standards)
- ✅ **TDD Foundation** (RED-GREEN-REFACTOR at every layer)
- ✅ **Security by Design** (multi-tenant isolation, secrets protection, audit trails)
- ✅ **Event-Driven Observability** (business events, structured logging, compliance)
- ✅ **API Standards** (REST design, error handling, versioning)
- ✅ **Technology Agnostic** (pseudocode core, swappable implementations)
- ✅ **Dual Licensing** (GPL v3 community + commercial)
- ✅ **Phased Rollout** (sprint-based delivery, quality gates)

**Where:** Every implementation follows organizational development rules  
**Why:** Proven patterns, reduced risk, organizational consistency, quality assurance  
**When:** From Sprint 1.1 forward, no exceptions  

---

## Architectural Foundation: Six-Tier Separation

Our server control framework implements the mandatory six-tier pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│ Tier 1: CLI/Scheduler Layer (Presentation)                     │
│ - Bash scripts, cron jobs, manual entry points                 │
│ - Framework-agnostic (no business logic, no database access)   │
│ - Delegates to Controllers via REST API                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Tier 2: Controller Layer (Orchestration)                       │
│ - Decision makers (pseudocode controllers)                      │
│ - State management, request routing                             │
│ - Zero execution, 100% delegation to Executors                │
│ - Framework-agnostic (Java Spring Boot → Rust → C++ later)    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Tier 3: REST API Layer (@RestController, Spring Boot)          │
│ - HTTP endpoints, request validation, response formatting      │
│ - Implements pseudocode interface                               │
│ - Delegates to Services for business logic                     │
│ - One structured error model across all endpoints              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Tier 4: Service Layer (@Service, Business Logic)               │
│ - Pseudocode executor: implements "what to do"                 │
│ - Rules, validation, orchestration                              │
│ - Delegates to Repositories for data access                    │
│ - Emits business events ONLY after transaction succeeds        │
│ - Records actor and correlation context for every write        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Tier 5: Repository Layer (@Repository, Data Access)            │
│ - ALL database operations ONLY here (SQL, queries, persistence)│
│ - Scopes all queries by tenant/security context                │
│ - Cross-tenant isolation enforced BEFORE repository access     │
│ - Zero business logic, zero service logic                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Tier 6: Database Layer & External Systems                      │
│ - Configuration storage (files, git, database)                 │
│ - Audit trail persistence                                       │
│ - Adapter implementations (platform-specific bash scripts)     │
└─────────────────────────────────────────────────────────────────┘
```

### Tier Responsibilities (Mapped to Our Framework)

#### Tier 1: CLI/Scheduler Presentation
```
Responsibilities:
- User interface (CLI, cron, webhook receivers, API clients)
- Accept user input
- Display results/status
- Schedule/trigger execution

Rules:
✅ Zero business logic
✅ No framework coupling (no Spring imports)
✅ Delegates ALL decisions to Controllers
✅ Can be Bash, Python, Node.js, anything

Violates:
❌ Hardcoding firewall rules
❌ Making approval decisions
❌ Querying database directly
```

#### Tier 2: Controller Layer (Pseudocode)
```
Responsibilities:
- Orchestration logic (what should happen, in what order)
- Decision-making without execution
- State management
- Routing to Executors

Rules:
✅ Framework-agnostic (language can change)
✅ Speaks to Executors via defined interface
✅ No actual command execution
✅ Can be swapped between implementations

Violates:
❌ Executing bash commands directly
❌ Accessing database
❌ Importing platform-specific libraries
```

#### Tier 3: REST API Layer
```
Responsibilities:
- HTTP endpoint definitions
- Request validation
- Response formatting
- Error handling

Rules:
✅ One structured error model (code, ID, timestamp, severity)
✅ Authorization enforced at API layer
✅ Input validation at boundaries
✅ Delegates to Services

Violates:
❌ Business logic in endpoint handlers
❌ Multiple error response formats
❌ Database queries in controllers
```

#### Tier 4: Service Layer (Executor Logic)
```
Responsibilities:
- Pseudocode execution (translating "what" to "how")
- Business rules, validation
- Transactions, atomicity
- Event emission ONLY after success

Rules:
✅ Injects adapters via dependency injection
✅ No direct bash script instantiation (use adapters)
✅ Records actor and context for audit
✅ Emits events ONLY after DB commit

Violates:
❌ Direct file system access (use Repository)
❌ Creating adapter instances directly (inject them)
❌ Executing bash without logging
```

#### Tier 5: Repository Layer (Data Access)
```
Responsibilities:
- ALL database/persistence operations
- Query scoping by tenant/security context
- Enforcement of access boundaries

Rules:
✅ Scope EVERY query by tenant BEFORE repository access
✅ Check authorization in Repository
✅ SQL queries ONLY here
✅ No business logic

Violates:
❌ Querying without tenant scope
❌ Business logic in queries
❌ Instantiating repositories in Services
```

#### Tier 6: Database & Adapters
```
Responsibilities:
- Persistent storage (configuration, audit logs, state)
- Platform-specific implementations (bash adapters)
- External system integration

Rules:
✅ Only accessed through Repository
✅ Adapters are pure implementations (follow pseudocode)
✅ Bash scripts have clear entry points

Violates:
❌ Direct database access from other layers
❌ Adapter-specific logic in Services
```

---

## Dependency Injection (MANDATORY)

Following organizational standards, **ZERO direct instantiation**:

```java
// ❌ WRONG - Direct instantiation
@Service
public class FirewallService {
    private final FirewallAdapter adapter = new LinuxFirewallAdapter();  // BAD
    
    public void deployRule(Rule rule) {
        adapter.deploy(rule);  // Tightly coupled
    }
}

// ✅ CORRECT - Constructor injection
@Service
public class FirewallService {
    private final FirewallAdapter adapter;
    
    @Autowired
    public FirewallService(FirewallAdapter adapter) {
        this.adapter = adapter;  // Injected, testable, replaceable
    }
    
    public void deployRule(Rule rule) {
        adapter.deploy(rule);  // Loosely coupled
    }
}
```

This enables:
- **Swapping implementations** (Linux → macOS → Windows at runtime)
- **Testing** (mock adapters in tests)
- **Multi-tenancy** (different adapters per tenant if needed)

---

## Pseudocode as Business Logic Layer

The core innovation: **Pseudocode defines Tier 4 (Service) behavior**

```java
// Pseudocode (language-agnostic)
function deployFirewallRules(rules[], target) {
    validate(rules)
    authorizeUser()
    
    transaction {
        rules.forEach(rule -> {
            rule.validate()
            rule.scope = getCurrentTenant()  // Multi-tenant
            persistRule(rule)
        })
        
        adapter.deploy(rules)  // Delegates to adapter
        
        logAuditTrail(actor, "deployed", rules)
    }
    
    emitEvent("FirewallRulesDeployed", rules)  // After success
    
    return success
}

// Spring Boot Implementation (Tier 4 Service)
@Service
public class FirewallService {
    private final FirewallRepository repository;
    private final FirewallAdapter adapter;
    private final AuditService audit;
    private final EventPublisher events;
    
    @Transactional
    public void deployFirewallRules(List<FirewallRule> rules) {
        // Validation
        rules.forEach(this::validate);
        authorizeCurrentUser();
        
        // Persist (scoped by tenant automatically)
        List<FirewallRule> persisted = repository.saveAll(rules);
        
        // Deploy (via adapter - could be Linux, macOS, AWS, etc.)
        adapter.deploy(persisted);
        
        // Audit
        audit.log(currentUser(), "deployed", persisted);
        
        // Event (ONLY after transaction succeeds)
        events.publish(new FirewallRulesDeployed(persisted));
    }
}

// Future Rust Implementation (same pseudocode, different language)
impl FirewallService {
    pub async fn deploy_firewall_rules(&self, rules: Vec<FirewallRule>) {
        // Same logic, Rust syntax
        // Adapter could be Linux, macOS, Windows, AWS...
    }
}
```

**Benefit:** Change Java → Rust tomorrow, pseudocode stays identical

---

## Multi-Tenant Isolation (MANDATORY)

Following organizational security standards, **zero cross-tenant leakage**:

### Rule 1: Scope at Repository Boundary
```java
@Repository
public class FirewallRuleRepository {
    @Query("SELECT * FROM firewall_rules WHERE tenant_id = ?1")
    public List<FirewallRule> findByTenant(String tenantId) {
        // Every query scoped by tenant BEFORE database access
    }
}
```

### Rule 2: Validate Before Repository
```java
@Service
public class FirewallService {
    public FirewallRule getRule(String ruleId) {
        // Validate user has access to rule
        String userTenant = SecurityContext.getCurrentTenant();
        FirewallRule rule = repository.findById(ruleId);
        
        if (!rule.getTenant().equals(userTenant)) {
            throw new AccessDeniedException("Cross-tenant access");  // Blocked
        }
        
        return rule;  // Safe
    }
}
```

### Rule 3: Test Cross-Tenant Denial (MANDATORY)
```java
@Test
public void userFromTenantA_cannotAccess_TenantB_rules() {
    // Create user in Tenant A
    User userA = createUserInTenant("tenant-a");
    
    // Create rules in Tenant B
    FirewallRule ruleB = createRuleInTenant("tenant-b");
    
    // Attempt access (should fail)
    assertThatThrownBy(() -> 
        service.getRule(ruleB.getId(), userA)
    ).isInstanceOf(AccessDeniedException.class);
}
```

---

## Test-Driven Development (MANDATORY)

Following organizational TDD standards: **RED-GREEN-REFACTOR at every layer**

### Layer 1: Unit Tests (Controller Logic)
```java
@Test
public void deployFirewallRules_validates_before_deploy() {
    // RED: Test that fails (behavior not implemented)
    List<FirewallRule> invalidRules = List.of(
        new FirewallRule(null, "invalid")  // Missing source IP
    );
    
    // GREEN: Make it pass
    assertThatThrownBy(() -> 
        service.deployFirewallRules(invalidRules)
    ).isInstanceOf(ValidationException.class);
    
    // REFACTOR: Improve implementation while keeping test passing
}

@Test
public void deployFirewallRules_records_audit_trail() {
    FirewallRule rule = validRule();
    
    service.deployFirewallRules(List.of(rule));
    
    // Assert audit trail was recorded
    AuditTrailEntry entry = auditService.getLastEntry();
    assertThat(entry.getActor()).isEqualTo(currentUser());
    assertThat(entry.getAction()).isEqualTo("deployed");
    assertThat(entry.getTenant()).isEqualTo(currentTenant());
}
```

### Layer 2: Layer Tests (Service → Repository)
```java
@Test
public void deployFirewallRules_persists_and_deploys() {
    FirewallRule rule = validRule();
    
    service.deployFirewallRules(List.of(rule));
    
    // Assert persisted
    assertThat(repository.findById(rule.getId())).isNotEmpty();
    
    // Assert deployed (via mock adapter)
    verify(adapter).deploy(List.of(rule));
}
```

### Layer 3: Adapter Tests (Spring Boot → Bash)
```bash
#!/bin/bash
# Test: Linux firewall adapter deploys iptables rule

# RED: Test that fails
./deploy-rule.sh "allow-ssh" "0.0.0.0/0" "22" || echo "FAILED"

# GREEN: Make it pass
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# REFACTOR: Improve while keeping test passing
echo "✓ Rule deployed"
```

### Layer 4: Integration Tests (End-to-End)
```java
@Test
public void firewall_rule_deployment_e2e() {
    // Setup
    FirewallRule rule = validRule();
    
    // Execute: API call → Service → Adapter → Real system
    mockMvc.perform(post("/api/firewall/rules")
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(rule)))
        .andExpect(status().isCreated());
    
    // Verify: End-to-end success
    // - Rule persisted in database
    assertThat(repository.findById(rule.getId())).isNotEmpty();
    
    // - Adapter executed on real system (or mocked)
    // - Audit trail recorded
    // - Event published
}
```

---

## Event-Driven Observability (MANDATORY)

Following organizational standards: **business events drive compliance and monitoring**

### Rule 1: Emit Events ONLY After Transaction Success
```java
@Service
@Transactional
public class FirewallService {
    private final EventPublisher events;
    
    public void deployFirewallRules(List<FirewallRule> rules) {
        // Persist
        repository.saveAll(rules);
        
        // Transaction commits first
    }
    
    @TransactionalEventListener  // Fires AFTER transaction commits
    public void onRuleDeployed(FirewallRulesDeployed event) {
        events.publish(event);  // Only after success
    }
}
```

### Rule 2: Events Must Contain Stable Identity & Audit Context
```java
public class FirewallRulesDeployedEvent {
    private String aggregateId;        // Stable rule ID
    private String version;            // Rule version
    private String tenantId;           // Multi-tenant
    private String actor;              // Who did it
    private String correlationId;      // Trace across systems
    private Instant timestamp;         // When
    
    // ❌ NEVER include secrets
    // ❌ NEVER include raw credentials
    // ✅ Include enough for audit and debugging
}
```

### Rule 3: Structured Logging (No Secrets)
```java
@Service
public class FirewallService {
    private final Logger log = LoggerFactory.getLogger(this.class);
    
    public void deployFirewallRules(List<FirewallRule> rules) {
        // ✅ CORRECT: Structured logging with audit context
        log.info("Deploying firewall rules",
            "tenant_id", currentTenant(),
            "correlation_id", correlationId(),
            "actor", currentUser(),
            "rule_count", rules.size());
        
        // ❌ WRONG: Secrets in logs
        // log.info("API key: " + apiKey);  // NEVER
        
        // ❌ WRONG: Full rule object (might contain secrets)
        // log.info("Rules: " + rules);  // NEVER
    }
}
```

---

## API Standards (MANDATORY)

Following organizational REST standards:

### Single Error Model (Across All Endpoints)
```java
@RestController
@RequestMapping("/api/firewall")
public class FirewallController {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "VALIDATION_FAILED",        // Stable code
            "fw-rule-001",              // Error ID
            Instant.now(),              // Timestamp
            "ERROR",                    // Severity
            "firewall-service",         // Source
            Map.of("field", "source_ip", "reason", "invalid CIDR")  // Details
        ));
    }
}
```

### Authorization at API + Service Layers
```java
@RestController
@RequestMapping("/api/firewall")
public class FirewallController {
    
    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")  // API-level check
    public ResponseEntity<FirewallRuleDto> deploy(@RequestBody FirewallRuleDto dto) {
        return ResponseEntity.created(
            service.deployFirewallRules(dto)  // Service checks again
        ).build();
    }
}

@Service
public class FirewallService {
    public void deployFirewallRules(FirewallRuleDto dto) {
        // API layer already checked, but Service layer ALSO validates
        authorizeCurrentUser();  // Defense-in-depth
        
        // ... business logic ...
    }
}
```

---

## Versioning & Change Discipline (MANDATORY)

Following organizational versioning standards:

### Semantic Versioning (Pseudocode)
```
Pseudocode Version: 1.2.3
                    ↑ ↑ ↑
                    │ │ └─ PATCH (adapter bug fixes, no API change)
                    │ └──── MINOR (new capabilities, backward compatible)
                    └────── MAJOR (breaking changes to pseudocode interface)
```

### Implementation Versions Track Independently
```
Pseudocode: 1.2.3
├── Spring Boot impl: 1.2.3
├── Rust impl:       1.1.5 (started later)
└── C++ impl:        2.0.1 (already implemented v2)

All versions implement same pseudocode, but can differ if behind
```

### Changelog Example
```
## [1.2.0] - 2026-08-15
### Added
- FirewallRulesDeployed event published after successful deployment
- Cross-tenant denial test (mandatory security gate)
- Audit trail recording for all rule changes

### Changed
- Rules now scoped by tenant in Repository layer (was in Service)
- Error response format standardized across all endpoints

### Fixed
- Firewall rules not persisting transaction rollback (Issue #42)

### Deprecated
- Legacy `/api/v1/rules` endpoint (use `/api/v2/rules`)
```

---

## Security Policy (MANDATORY)

Following organizational security standards:

### Tier 1: Authentication
- JWT tokens for API access
- OAuth2 for third-party integrations
- MFA for privileged operations (deployment, rule changes)

### Tier 2: Authorization
- RBAC (Role-Based Access Control)
- Multi-tenant isolation (enforced at Repository)
- Principle of least privilege

### Tier 3: Encryption
- TLS 1.3 for all API endpoints
- AES-256 for at-rest configuration storage
- Secrets encrypted before persistence

### Tier 4: Audit & Compliance
- Every write operation logged with actor + context
- Immutable audit trail (append-only)
- Compliance reports auto-generated

### Tier 5: Input Validation
- All external input validated at API boundary
- Regex patterns for rules (prevent injection)
- Rate limiting on deployment endpoints

### Tier 6: Secrets Protection (MANDATORY)
```
❌ NEVER:
- Store credentials in config files
- Include tokens in logs
- Expose secrets in error messages
- Commit .env or credentials files
- Pass secrets as command arguments

✅ ALWAYS:
- Use encrypted secret storage
- Rotate tokens automatically
- Sanitize logs before output
- Use environment variables
- Audit authentication events
```

---

## Phased Implementation (Sprint Strategy)

Following organizational scrum standards:

### Sprint Duration: 1 Week
- Monday: Planning (TDD specifications)
- Tuesday-Thursday: Implementation (RED-GREEN-REFACTOR)
- Friday: Review & Retrospective

### Quality Gates (Mandatory Before Merge)
1. **TDD Compliance**: All code has RED test first
2. **Layer Tests Pass**: Unit, layer, integration tests
3. **Cross-Tenant Denial Tests**: Prove isolation works
4. **Secrets Not Exposed**: Assert no credentials in output
5. **SonarQube Green**: Coverage targets met, security issues zero
6. **Code Review**: Two approvals required
7. **CI/CD Pipeline**: All builds pass

### Success Metrics Per Phase
| Phase | Test Coverage | Security Issues | Uptime | Customers |
|-------|---------------|-----------------|--------|-----------|
| 1 | 90%+ | 0 | N/A | Internal |
| 2 | 90%+ | 0 | 99% | 10+ |
| 3 | 85%+ | 0 | 99.5% | 50+ |
| 4 | 85%+ | 0 | 99.5% | 100+ |
| 5 | 80%+ | 0 | 99.9% | 250+ |
| 6 | 80%+ | 0 | 99.99% | 500+ |

---

## Technology Stack (Swappable)

### Current Implementation
- **Language**: Java
- **Framework**: Spring Boot
- **Database**: PostgreSQL (file-based git during Phase 1-4)
- **Testing**: JUnit 5, Mockito, Spring Test
- **CI/CD**: Jenkins, SonarQube
- **Deployment**: Docker, Kubernetes-ready

### Future Implementations (No Pseudocode Change)
- **Rust**: High-performance, memory-safe implementation
- **C++**: Low-latency, embedded-system version
- **PHP**: Simple, LAMP-stack compatible
- **Bash**: Standalone, no dependencies version

**Key:** Implementation changes, pseudocode stays identical

---

## Dual Licensing Integration

### Open Source (GPL v3)
- Pseudocode lives on GitHub (public)
- Community contributions welcome
- Strict TDD enforced via CI/CD
- Improvements flow back

### Commercial License
- Enterprise adapters (AWS, Azure, private cloud)
- Priority support SLA
- Commercial features (advanced monitoring, reporting)
- Annual subscription

**Revenue Model:**
- Open source: Community adoption & ecosystem
- Commercial: Enterprise revenue & sustainability

---

## Success Vision (Perfect State)

### From Organizational Perspective
✅ **Architecture**: Follows six-tier separation, no layer violations  
✅ **Testing**: 90%+ coverage, zero flaky tests, TDD on every feature  
✅ **Security**: Multi-tenant isolation proven, zero cross-tenant leaks  
✅ **Events**: Business events drive compliance, audit trail complete  
✅ **API**: Unified error model, consistent versioning, rate limiting  
✅ **Quality**: SonarQube A rating, zero security issues  
✅ **Compliance**: GDPR, HIPAA, SOC2, PCI-DSS certifications  

### From Developer Perspective
✅ **Clear responsibility**: Each tier has one job, no crossover  
✅ **Easy to test**: Dependency injection, mock adapters, deterministic tests  
✅ **Easy to extend**: Add new platform → write adapter, implement pseudocode  
✅ **Hard to break**: Layer violations caught by tests, CI/CD blocks bad code  
✅ **Fun to work on**: Clear patterns, fast feedback, proven practices  

### From Customer Perspective
✅ **Reliable**: Firewall rules deploy flawlessly, zero downtime  
✅ **Secure**: Cross-tenant isolation guaranteed, audit trail trusted  
✅ **Responsive**: API < 10ms, deployment < 30 seconds  
✅ **Compliant**: Automatically meets GDPR, HIPAA, SOC2 requirements  
✅ **Transparent**: Audit trail proves exactly what happened, when, who  

---

## What This Achieves

| Goal | How Achieved | Organizational Benefit |
|------|-------------|----------------------|
| **No Layer Violations** | TDD tests enforce separation, CI blocks violations | Code quality, maintainability |
| **Zero Security Incidents** | Multi-tenant tests mandatory, secrets never exposed | Compliance, customer trust |
| **Easy to Change Tech** | Pseudocode defines behavior, adapters are swappable | Future-proof, no lock-in |
| **Auditable Operations** | Every action logged with actor+context, events emit | Compliance, forensics |
| **Testable Everything** | Dependency injection, deterministic tests | Confidence, regression prevention |
| **Scalable Safely** | Proven patterns from organization, TDD catches issues | Enterprise adoption |

---

## Next Steps

1. **Sprint 1.1** (Pseudocode Design)
   - Define controller/executor interface (tied to architecture)
   - Write tests FIRST (RED phase)
   - Create pseudocode spec

2. **Sprint 1.2** (Spring Boot Implementation)
   - Implement Tier 3-4 (REST API + Services)
   - All TDD (RED-GREEN-REFACTOR)
   - Deploy to Jenkins, SonarQube gates mandatory

3. **Sprint 1.3** (First Adapter)
   - Linux/iptables firewall adapter
   - Layer tests prove adapter correctness
   - Cross-tenant denial tests prove security

4. **Phase 2+** (Expand & Sustain)
   - More adapters (macOS, Windows, AWS)
   - More capabilities (apps, resources, monitoring)
   - Continuous quality gates, zero technical debt

---

## References

**Organizational Standards Used:**
- [Architecture & Layering](../../project-settings/common/originals/development-rules/ARCHITECTURE_AND_LAYERING.md)
- [TDD](../../project-settings/common/originals/development-rules/TDD.md)
- [Security & Isolation](../../project-settings/common/originals/development-rules/SECURITY_AND_ISOLATION.md)
- [Events & Observability](../../project-settings/common/originals/development-rules/EVENTS_AND_OBSERVABILITY.md)
- [API Design](../../project-settings/common/originals/development-rules/API_DESIGN.md)

**Dream Architecture Reference:**
- [Dream Architecture](./dream-architecture.md)

**Implementation Roadmap:**
- [Implementation Roadmap](./implementation-roadmap.md)

---

**Status:** Ready for Sprint 1.1  
**Last Updated:** 2026-07-21  
**Authority:** Common Standards + Dream Vision = Structured Reality
