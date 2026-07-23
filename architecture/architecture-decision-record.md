architecture-decision-record.md

# Server Control Architecture - Key Decisions

## Core Philosophy
- **Platform-Agnostic Design**: Pseudocode for logic, adapters for platform-specific implementations
- **Server Configurations**: Scripts stored in files and/or database, not buzzwords like IaC
- **Separation of Concerns**: Controllers orchestrate, Executors implement. A local controller can also execute.
- **Technology Stack is Replaceable**: Language/framework is implementation detail, not architecture
  - Today: JavaScript, tomorrow: Rust, next week: C++, Java, PHP, Bash
  - Pseudocode remains constant; implementations swap freely
  - No technology lock-in

## Design Principles
1. Controllers execute nothing — they decide and orchestrate
2. Executors do the actual work
3. Local controllers can also be executors (flexible architecture)
4. Platform adapters translate pseudocode to bash/platform-specific commands
5. UI clients remain thin and dumb — no domain/business decision logic in UI

## UI Client Principle (MANDATORY)
- Angular clients are presentation-only and API-consumption-only.
- All business rules, orchestration, authorization decisions, and policy evaluation live in backend layers.
- UI may perform input validation for UX, but never authoritative validation for security or policy.
- UI must not bypass backend contracts or call adapters/executors directly.

## Delivery Method (MANDATORY)
- Development is strict top-down TDD.
- Apply iterative TDD one boundary at a time.
- Sequence per capability: API contract test -> controller orchestration test -> service rule test -> repository/adapter test.
- Do not implement deeper layers before the boundary test above them fails first.

## Scope - All Areas (Phased)
- [x] Process/service management
- [x] System resources (CPU, memory, disk)
- [x] Networking & firewalls
- [x] User & permissions management
- [x] Application deployment & updates
- [x] Monitoring & logging
- [x] Package management

## Implementation Phases
1. **Phase 1: Defence & Sandboxing** (Foundational)
2. **Phase 2: Firewall** (First concrete task)
3. Phase 3+: Resource management, deployments, monitoring, etc.

## Primary Use Case
- **Self-hosted app management**
- Proxy control (nginx, apache, true proxy servers) as adapters to pseudocode
- Manage apps safely in isolated environments

## Architecture Layers
```
Pseudocode Controllers (decision/orchestration)
    ↓
Pseudocode Executors (action logic)
    ↓
Adapters (platform translation)
    ↓
Platform-specific scripts (bash, PowerShell, etc.)
```

## Configuration Storage
- Scripts can live in files (git-versioned)
- Scripts can live in a database (dynamic, updateable)
- Or both (database as source of truth, files as cache)

## Key Architectural Patterns
- Strategy pattern (pseudocode = strategy, adapters = implementations)
- Adapter pattern (platform-specific implementations)
- Separation of Concerns (controller vs executor)
- Optional Agent pattern (local or remote executors)

## Execution Model (Context-Dependent)
- **Not prescribed by framework** — implementer chooses based on risk tolerance
- **Defence/regulated sectors**: Manual approval gates, full audit trails, minimal automation
- **High-velocity environments**: Webhooks, cron schedules, self-service automation
- **Healthcare/compliance**: Approval workflows, compliance logging, scheduled execution
- **Framework must support all models** — execution mechanism is pluggable

## Configuration Storage
- **Phase 1-4 (current)**: Filesystem-based storage
  - Bash scripts in `scripts/` folder (external)
  - Configuration in files (git-versioned for audit trail)
  - Simple, version-controlled, no database dependency
- **Phase 5+**: Database storage (future enhancement)
  - More secure, dynamic updates without file changes
  - Better for distributed/cloud deployments
  - Decision deferred for now

## Execution Triggers & Deployment (TBD Per Context)
- Execution model selection (manual, cron, webhook, API, daemon)
- Approval workflow design (defence vs. grocery store needs differ)
- Audit trail requirements
- Rate limiting/throttling needs

## Starting Technology Stack
- **Language**: Java
- **Framework**: Spring Boot
- **User Interface**: Angular (definitive choice)
- **UI Integration Model**: Angular SPA consuming Spring Boot REST APIs
- **Note**: This is implementation v1. Pseudocode remains constant; future implementations can use Rust, C++, PHP, Bash, etc.
- **Target platforms for adapters (near-term)**: Linux, Windows, AWS, GCP, Azure
- **macOS**: Not in current implementation scope

## Testing & Quality Strategy
- **Strict TDD (Test-Driven Development)**
  - Tests define behavior before implementation
  - Top-down: start from boundary/contract tests and iterate downward
  - One boundary at a time (do not open multiple unfinished boundaries)
  - Pseudocode must pass test suite
  - Adapters must pass platform-specific tests
  - No feature without passing tests
- **Testing at each layer**:
  - Pseudocode logic tests (unit tests, behavior verification)
  - Adapter tests (platform-specific, can be integration tests)
  - End-to-end tests (pseudocode → adapter → real system)
- **Verification is critical** — especially for security/defence operations
- **Strategy details TBD** — will emerge during Sprint 1.1 & 1.2
