# GS-Server Present Architecture Overview

**Project:** GS-Server  
**Version:** 1.0 (Present State)  
**Last Updated:** 2026-07-24  
**Status:** 78 source files, 12 test files, 3 main modules

---

## Vision & Purpose

GS-Server is a unified server management platform that enables operators to:
- **Harden** servers (Linux/Windows) with baseline and strict security profiles
- **Configure proxies** (nginx, Apache) with upstream server routing
- **Manage users** with role-based access control (RBAC)
- **Access terminals** via interactive WebSocket for command execution
- **Track operations** with full history and rollback capability

**Target users:** DevOps operators, system administrators, infrastructure teams

---

## High-Level Architecture

### Three-Module Design

```
GS-Server (unified backend for multi-tenant infrastructure)
├── GSServer-db
│   └── File-based persistence layer (JSON + filesystem)
│
├── GSServer-jar
│   └── Spring Boot 3.3.2 standalone application (NOT YET WIRED)
│
└── GSServer-UI
    ├── Controllers (REST API endpoints @Controller)
    ├── Services (business logic @Service)
    ├── Adapters (platform-specific execution)
    ├── Security (authentication, RBAC, authorization)
    ├── Terminal (WebSocket for interactive access)
    └── Angular SPA frontend
```

**Current reality:** All functionality is in GSServer-UI. GSServer-jar exists but is NOT integrated into the request path. UI calls services directly instead of via REST HTTP.

---

## Core Capabilities & Status

### 1. Hardening (95% Complete)

**What it does:**
- Applies security hardening profiles (baseline, strict) to Linux and Windows servers
- Executes platform-specific shell scripts
- Tracks operation history with rollback
- Validates tenant/operator authorization

**Implementation:**
- ✅ HardeningController (REST @Controller, 202 Accepted)
- ✅ DefaultHardeningService (orchestration + validation)
- ✅ LinuxHardeningAdapter (bash scripts, real execution)
- ✅ WindowsHardeningAdapter (PowerShell scripts, real execution)
- ✅ ProcessHardeningCommandExecutor (ProcessBuilder, timeout handling)
- ✅ InMemoryHardeningOperationStateStore (memory + file persistence)
- ✅ FileBasedHardeningOperationStateRepository (JSON on disk)
- ✅ Comprehensive test suite (8 tests, 100% passing)

**Data flow:**
```
POST /api/v1/hardening
  ↓ HardeningController
  ↓ HardeningService.triggerHardening(request)
    ├─ Validate tenant, operator, profile
    ├─ Detect platform (Linux or Windows)
    └─ Call adapter
      ├─ LinuxHardeningAdapter.applyBaselineHardening()
      │   └─ Shell script via ProcessBuilder (timeout: 2 min)
      └─ Or WindowsHardeningAdapter.applyStrictHardening()
          └─ PowerShell script via ProcessBuilder (timeout: 2 min)
  ├─ On success: persist state, return 202 Accepted
  └─ On failure: rollback, persist failed state, throw HardeningExecutionException
```

**State model:**
```json
{
  "operationId": "uuid",
  "occurredAtUtc": "2026-07-24T10:00:00.000Z",
  "status": "success|failed",
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "profile": "baseline|strict",
  "platform": "linux|windows",
  "exitCode": 0,
  "timedOut": false,
  "rollbackStatus": "succeeded|failed",
  "stderr": "error output if failed"
}
```

---

### 2. Gateway Proxy (50% Complete)

**What it does:**
- Configures nginx as a reverse proxy
- Routes upstream traffic to configurable backends
- Tracks proxy configuration changes
- Supports rollback to prior states

**Implementation:**
- ✅ GatewayProxyController (REST @Controller, 202 Accepted)
- ✅ DefaultGatewayProxyService (wired executor, real command execution)
- ✅ ProcessNginxCommandExecutor (real nginx commands, not stubbed)
- ✅ InMemoryGatewayProxyOperationStateStore (memory + file)
- ✅ FileBasedGatewayProxyOperationStateRepository (JSON on disk)
- ✅ Comprehensive test suite (4 tests, 100% passing)
- ❌ Apache proxy adapter (planned, not implemented)
- ❌ Firewall rules enforcement (not in scope yet)

**Data flow:**
```
POST /api/v1/gateway/proxy/configure
  ↓ GatewayProxyController
  ↓ GatewayProxyService.configureNginxProxy(request)
    ├─ Validate tenant, operator, upstream host/port
    ├─ Build nginx command
    └─ Call NginxCommandExecutor
      └─ ProcessNginxCommandExecutor.execute(command)
        └─ nginx -t, nginx -s reload (real commands)
  ├─ On success: persist state, return 202 Accepted
  └─ On failure: persist failed state, throw GatewayProxyExecutionException

GET /api/v1/gateway/proxy/latest
  ↓ Returns latest proxy configuration state

POST /api/v1/gateway/proxy/rollback/{operationId}
  ↓ Restores prior nginx configuration from history
```

**State model:**
```json
{
  "operationId": "uuid",
  "occurredAtUtc": "2026-07-24T10:00:00.000Z",
  "status": "success|failed",
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "enabled": true|false,
  "upstreamHost": "192.168.1.100",
  "upstreamPort": 8080,
  "tlsEnabled": true|false,
  "message": "Operation result message"
}
```

---

### 3. Proxy Installation (50% Complete)

**What it does:**
- Provides installation instructions for proxy setup
- Detects if nginx or Apache is running
- Handles proxy runtime status checks

**Implementation:**
- ✅ ProxyInstallationController
- ✅ ProxyInstallationService (installation guide generation)
- ⚠️ ProxyService (status detection only, no real commands)

**Status:**
- ✅ Installation guide HTML generation
- ⚠️ Runtime detection (checks process names, not actual commands)
- ❌ Actual installation execution (stubbed only)

---

### 4. User Management (90% Complete)

**What it does:**
- Create, read, update, delete user accounts
- Assign role-based permissions (4 groups)
- Prevent self-lockout and last-superadmin deletion
- Password hashing with bcrypt

**Implementation:**
- ✅ UserManagementController (REST @Controller)
- ✅ UserManagementService (CRUD + validation)
- ✅ JsonUserStore (file-based persistence, JSON)
- ✅ JsonUserDetailsService (Spring Security integration)
- ✅ ManagedUser (domain model)

**Roles (4 groups):**
1. **GROUP_HARDENING_OPERATORS** - Read/Write hardening, read proxy
2. **GROUP_HARDENING_ADMINS** - Manage operators, read/write all ops
3. **GROUP_AUDIT_READERS** - Read-only audit/operation logs
4. **GROUP_SUPERUSER** - Full access, user management

**Authorization:**
- @PreAuthorize on every endpoint
- Tenant validation in service layer
- Last-superadmin guard (prevent lockout)

---

### 5. Authentication & Security (85% Complete)

**What it does:**
- HTTP Basic authentication (username/password)
- Passwordless "thor" user (localhost only, sudoers)
- Method-level security (@PreAuthorize)
- CORS configuration for Angular frontend
- WebSocket ticket-based auth for terminal

**Implementation:**
- ✅ SecurityConfig (Spring Security configuration)
- ✅ LocalhostThorAuthenticationProvider (passwordless thor)
- ✅ JsonUserDetailsService (Spring User integration)
- ✅ JsonUserStore (user load from JSON)
- ✅ CorsConfig (CORS headers)
- ❌ Encrypted passwords (current: plain JSON storage in dev)
- ❌ OAuth2 (planned for Phase 6+)
- ❌ MFA (planned for Phase 6+)

**Current auth flow:**
1. POST /api/v1/auth/config → returns auth config (public endpoint)
2. Browser → Spring SecurityFilterChain
3. Spring matches @PreAuthorize annotations
4. LocalhostThorAuthenticationProvider allows "thor" from localhost
5. All other users require password (bcrypt verified)

---

### 6. Terminal Access (70% Complete)

**What it does:**
- Provides interactive terminal access via WebSocket
- Supports real pty4j interactive sessions
- One-time ticket-based authentication
- Full command history and output

**Implementation:**
- ✅ TerminalWebSocketHandler (WebSocket message routing)
- ✅ TerminalSessionManager (session lifecycle)
- ✅ InteractiveTerminalSession (pty4j-based terminal)
- ✅ TerminalTicketService (one-time auth tickets)
- ✅ WebSocketConfig (WebSocket path registration)
- ⚠️ Terminal output capture (partial, not all modes)
- ❌ Terminal audit logging (commands not logged)

**WebSocket flow:**
```
GET /api/v1/terminal/ws?ticket=one-time-token
  ↓ TerminalAuthHandshakeInterceptor validates ticket
  ↓ TerminalWebSocketHandler.afterConnectionEstablished()
  ↓ Creates InteractiveTerminalSession (real pty4j)
  ↓ bidirectional message streaming

User types: command
  ↓ TerminalWebSocketHandler.handleTextMessage()
  ↓ InteractiveTerminalSession.sendInput(text)
  ↓ pty4j writes to process
  ↓ Process executes, outputs result
  ↓ pty4j reads output
  ↓ Server sends to client via WebSocket
```

---

## Layer Architecture (Current)

```
┌─────────────────────────────────────────────────────────────┐
│ HTTP REST Layer                                             │
│ @RestController classes: HardeningController, etc.          │
│ • Route HTTP requests                                       │
│ • Validate format (@PreAuthorize for auth)                 │
│ • Accept/reject with appropriate status codes              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Service/Business Logic Layer                                │
│ @Service classes: DefaultHardeningService, etc.             │
│ • Validate business rules (tenant, operator, profile)      │
│ • Orchestrate adapters                                      │
│ • Persist state via repository                             │
│ • Handle errors and return domain objects                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Execution/Adapter Layer                                     │
│ LinuxHardeningAdapter, ProcessNginxCommandExecutor, etc.   │
│ • Platform-specific execution                              │
│ • ProcessBuilder for shell commands                        │
│ • Command building and result parsing                      │
│ • No business logic, no validation                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Data Persistence Layer                                      │
│ FileBasedHardeningOperationStateRepository, etc.            │
│ • JSON file I/O                                             │
│ • State serialization/deserialization                      │
│ • Directory management                                     │
└─────────────────────────────────────────────────────────────┘
```

**Current state:** Clean separation is mostly implemented. Controllers are thin (delegating), services handle validation, adapters handle execution, repositories handle persistence.

---

## Key Principles in Practice

### 1. ✅ Multi-Tenant Isolation
- Hard-coded tenant list: `tenant-a`, `tenant-b`
- Every operation must specify tenantId
- Service validates tenantId before execution
- State persisted with tenant context
- Cross-tenant access denied (validated in service)

### 2. ✅ Platform Abstraction
- LinuxHardeningAdapter and WindowsHardeningAdapter behind HardeningService
- Service doesn't know/care about platform details
- Adapter detection via `detectPlatform()` supplier
- Commands vary by platform, API stays unified

### 3. ✅ Fail-Safe Operations
- Full operation history in file-based JSON store
- State captured before and after operations
- Rollback capability (revert to prior operationId)
- Immutable history (append-only JSON files)

### 4. ⚠️ Technology Abstraction in Naming
- ✅ `HardeningController` - Good, abstracts platform
- ✅ `GatewayProxyService` - Good, abstracts proxy type
- ⚠️ Controllers called `ProxyController`, `HardeningController` (not "Handler" yet)
- ❌ `ProcessNginxCommandExecutor` - OK because executor is platform-specific, but naming breaks convention

### 5. ✅ Dependency Injection
- All components use constructor injection
- Spring @Autowired on constructors (not fields)
- Easy to mock for testing
- Clear dependencies visible

### 6. ⚠️ Thin Orchestration
- Controllers delegate to services ✅
- Services validate + delegate to adapters ✅
- Some services have complex orchestration logic ⚠️ (could be simplified)

---

## Testing Status

### Test Coverage (12 test files, mix of unit and integration)

| Component | Tests | Status | Coverage |
|---|---|---|---|
| Hardening | 4 files | ✅ Passing | Service + Adapter |
| Gateway Proxy | 2 files | ✅ Passing | Service + Controller |
| Proxy | 1 file | ✅ Passing | Service |
| Authentication | 1 file | ✅ Passing | Controller contract |
| Database | 1 file | ✅ Passing | Repository |
| Integration | 1 file | ✅ Passing | Spring context |
| **Total** | **12 files** | **✅ Passing** | ~50% line coverage |

### Missing Tests
- ❌ Cross-tenant denial tests (authorization not verified)
- ❌ Secrets protection tests (passwords in errors)
- ❌ Terminal security tests (ticket validation)
- ❌ E2E integration tests (full request flow)

---

## Data Persistence (Current)

### File-Based JSON Storage

**Location:** `data/` directory (project root or configurable)

**Structure:**
```
data/
├── hardening/
│   ├── operation-{operationId}.json
│   └── latest.json (symlink or copy of latest)
├── gateway-proxy/
│   ├── operation-{operationId}.json
│   └── latest.json
├── proxy/
│   ├── operation-{operationId}.json
│   └── latest.json
└── users/
    └── users.json (bcrypt-hashed passwords)
```

**Characteristics:**
- ✅ Full history (every operation persisted)
- ✅ Immutable (write once, no updates)
- ✅ Rollback capable (can read any prior state)
- ❌ Not indexed (linear file scan for queries)
- ❌ Not encrypted (plaintext JSON)
- ❌ No transaction support

---

## Current Gaps vs Target State

### Critical (Block Production)

| Gap | Impact | Phase Target |
|---|---|---|
| Secrets redaction | Credentials leak in errors | Phase 1 |
| Audit logging | No compliance trail | Phase 1 |
| Jar backend wiring | UI calls services directly, not via REST | Phase 1-2 |
| Cross-tenant tests | Authorization not verified by tests | Phase 2 |

### High-Priority (Block Phase 3)

| Gap | Impact | Phase Target |
|---|---|---|
| Structured error model | Generic errors, no error codes | Phase 2 |
| Correlation IDs | Can't trace requests across services | Phase 2 |
| TLS enforcement | Dev-only HTTP, no HTTPS prod policy | Phase 2 |

### Medium-Priority (Improve Quality)

| Gap | Impact | Phase Target |
|---|---|---|
| Terminal audit logging | Commands not logged | Phase 6 |
| Structured logging | No tenant/correlation context | Phase 6 |
| API specification | No OpenAPI/Swagger | Phase 2 |

---

## Dependencies & Versions

**Spring Boot:** 3.3.2  
**Java:** 11+ (as per Spring Boot 3.3)  
**Frontend:** Angular 16+  
**Key libraries:**
- Jackson (JSON serialization)
- Spring Security (authentication, @PreAuthorize)
- pty4j (terminal emulation)
- JUnit 5 (testing)
- Mockito (mocking)

---

## Deployment Notes

### Current Deployment
- Single monolithic Spring Boot jar (GSServer-UI)
- Embedded Tomcat on port 8080
- Angular SPA embedded in jar
- File-based storage (no database)
- HTTP Basic auth (development mode)

### Production Requirements
- ❌ HTTPS/TLS enforcement (not yet)
- ❌ OAuth2 integration (not yet)
- ❌ Database persistence (not yet)
- ❌ Secrets management (not yet)
- ❌ Audit logging export (not yet)

---

## Summary

**GS-Server present state (July 24, 2026):**
- **78 Java source files** implementing infrastructure automation
- **3 capabilities mostly complete:** Hardening (95%), Gateway Proxy (50%), User Mgmt (90%)
- **Clean architecture:** Controllers → Services → Adapters → Repositories
- **Multi-tenant ready:** Tenant validation in place, but not tested
- **Real execution:** ProcessBuilder for actual hardening and proxy commands
- **File-based persistence:** JSON storage, full history, rollback support
- **Missing:** Secrets protection, audit logging, jar wiring, structured errors

**Next phases:**
1. Phase 1: Secrets redaction + audit logging (weeks 1-2)
2. Phase 2: Jar wiring + structured errors (weeks 3-6)
3. Phase 3: Firewall control (weeks 7-12)
4. Phases 4-6: Applications, resources, observability (additional 16+ weeks)

