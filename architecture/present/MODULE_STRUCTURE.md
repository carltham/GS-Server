# GS-Server Module Structure (Present)

**Project:** GS-Server  
**Version:** 1.0 (Present State)  
**Last Updated:** 2026-07-24

---

## Multi-Module Maven Organization

GS-Server uses a parent-child POM structure for clean separation of concerns:

```
GS-Server-pom/
├── pom.xml (parent POM, Spring Boot 3.3.2, Java 11+)
│
├── GSServer-db/
│   ├── pom.xml (database/persistence layer)
│   └── src/main/java/com/gsserver/db/
│
├── GSServer-jar/
│   ├── pom.xml (standalone application - NOT WIRED YET)
│   └── src/main/java/com/gsserver/jar/
│
└── GSServer-UI/
    ├── pom.xml (web server + Angular SPA)
    ├── src/main/java/com/gsserver/ui/
    ├── src/main/angular/ (Angular frontend)
    └── src/test/java/ (test suites)
```

---

## Module Details

### 1. GSServer-db Module (Persistence Layer)

**Purpose:** File-based persistence for operation state

**Current contents (2 files):**

| Class | Purpose | Status |
|---|---|---|
| `OperationStateRepository<T>` | Interface defining persistence contract | ✅ Implemented |
| `FileBasedOperationStateRepository<T>` | Abstract base for JSON file I/O | ✅ Implemented |

**Concrete implementations (in GSServer-UI, not db):**
- `FileBasedHardeningOperationStateRepository`
- `FileBasedProxyOperationStateRepository`
- `FileBasedGatewayProxyOperationStateRepository`

**Design:**
```
OperationStateRepository<T> (interface)
    ↑
    └── FileBasedOperationStateRepository<T> (abstract)
            ↑
            ├── FileBasedHardeningOperationStateRepository
            ├── FileBasedProxyOperationStateRepository
            └── FileBasedGatewayProxyOperationStateRepository
```

**Persistence mechanism:**
- ObjectMapper (Jackson) for JSON serialization
- Files written to `data/{capability}/` directory
- One file per operation: `operation-{operationId}.json`
- Path-based storage, no indexes

**Issues:**
- ❌ Concrete repositories in wrong module (should be in db)
- ❌ No transaction support (write-only, no rollback on partial save)
- ❌ No encryption (plaintext JSON files)
- ❌ No query support (linear iteration for searches)

---

### 2. GSServer-jar Module (Standalone Application)

**Purpose:** Spring Boot 3.3.2 standalone JAR application

**Current contents (1 file):**

| Class | Purpose | Status |
|---|---|---|
| `GsServerJarApplication` | Spring Boot entry point | ✅ Stub |

**Status:** ⚠️ **NOT INTEGRATED**
- Application main class exists
- NOT used in current request path
- GSServer-UI is the actual running application
- JAR module has no controllers, services, or business logic
- No Maven dependencies beyond Spring Boot

**Purpose (from roadmap):** 
- Phase 1-2: Wire jar as backend REST server
- UI should call jar via HTTP, not direct injection
- This enables: horizontal scaling, backend-only deployments, technology diversity

**Why not wired yet:**
- Adds REST communication layer (slower for development)
- Requires API contract definitions
- Would need load balancing for multiple jar instances
- Current direct injection simpler for Phase 1 (hardening 95%, proxy 50%)

---

### 3. GSServer-UI Module (Main Application)

**Purpose:** Spring Boot web server + REST API + Angular SPA

**Largest module (78 source files, most code here)**

#### 3.1 Package Structure

```
com.gsserver.ui/
├── GsServerUiApplication                          [Spring Boot entry point]
│
├── api/                                            [API error handling]
│   ├── ApiErrorResponse                           [Unified error response DTO]
│   └── ApiExceptionHandler                        [Global exception handler]
│
├── auth/                                           [Authentication API]
│   ├── AuthController                             [Public auth config endpoint]
│   └── AuthIdentityResponse                       [Auth config DTO]
│
├── security/                                       [Security configuration]
│   ├── SecurityConfig                             [Spring Security setup]
│   ├── SecurityUsersProperties                    [Config properties for users]
│   ├── JsonUserStore                              [File-based user store]
│   ├── JsonUserDetailsService                     [Spring UserDetailsService]
│   ├── ManagedUser                                [Domain: user entity]
│   ├── LocalhostThorAuthenticationProvider        [Passwordless thor auth]
│   ├── RequestOriginUtils                         [Utility: detect localhost]
│   └── CorsConfig                                 [CORS header configuration]
│
├── admin/                                          [User management]
│   ├── UserManagementController                   [REST: create/read/update/delete users]
│   ├── UserManagementService                      [CRUD + validation + RBAC]
│   ├── CreateUserRequest                          [DTO]
│   ├── UpdateUserRequest                          [DTO]
│   ├── PasswordResetRequest                       [DTO]
│   └── UserSummary                                [DTO: user list response]
│
├── hardening/                                      [Server hardening (95%)]
│   ├── HardeningController                        [REST: POST /api/v1/hardening]
│   ├── HardeningService                           [Interface]
│   ├── DefaultHardeningService                    [Implementation]
│   ├── HardeningRequest                           [DTO]
│   ├── HardeningResponse                          [DTO: 202 Accepted response]
│   ├── HardeningOperationState                    [Domain: operation state]
│   ├── HardeningOperationStateStore               [Interface]
│   ├── InMemoryHardeningOperationStateStore       [Memory + file storage]
│   ├── HardeningExecutionException                [Custom exception]
│   ├── PolicyViolationException                   [Custom exception]
│   │
│   └── adapter/                                    [Execution layer]
│       ├── HardeningCommandExecutor               [Interface]
│       ├── ProcessHardeningCommandExecutor        [ProcessBuilder implementation]
│       ├── LinuxHardeningAdapter                  [Platform: Linux hardening]
│       ├── WindowsHardeningAdapter                [Platform: Windows hardening]
│       ├── CommandExecutionResult                 [DTO: process result]
│       └── HardeningExecutionReport               [DTO: hardening report]
│
├── gateway/                                        [Gateway proxy (50%)]
│   ├── GatewayProxyController                     [REST: configure/rollback proxy]
│   ├── GatewayProxyService                        [Interface]
│   ├── DefaultGatewayProxyService                 [Implementation + nginx executor wiring]
│   ├── GatewayProxyRequest                        [DTO]
│   ├── GatewayProxyResponse                       [DTO: 202 Accepted response]
│   ├── GatewayProxyOperationState                 [Domain: nginx state]
│   ├── GatewayProxyOperationStateStore            [Interface]
│   ├── InMemoryGatewayProxyOperationStateStore    [Memory + file storage]
│   ├── GatewayProxyExecutionException             [Custom exception]
│   │
│   └── adapter/                                    [Nginx execution]
│       ├── NginxCommandExecutor                   [Interface]
│       ├── ProcessNginxCommandExecutor            [ProcessBuilder implementation]
│       ├── NginxConfigurationCommand              [Command builder]
│       └── NginxExecutionResult                   [DTO: command result]
│
├── proxy/                                          [Proxy management (50%)]
│   ├── ProxyController                            [REST: apply/status proxy]
│   ├── ProxyService                               [Interface]
│   ├── DefaultProxyService                        [Status detection, no command exec]
│   ├── ProxyInstallationController                [REST: installation guide]
│   ├── ProxyInstallationService                   [Generate install HTML]
│   ├── ProxyRequest                               [DTO]
│   ├── ProxyResponse                              [DTO: 202 Accepted response]
│   ├── ProxyOperationState                        [Domain: proxy state]
│   ├── ProxyOperationStateStore                   [Interface]
│   ├── InMemoryProxyOperationStateStore           [Memory + file storage]
│   ├── ProxyRuntimeStatus                         [DTO: nginx/apache running?]
│   ├── SiteFileResponse                           [DTO]
│   ├── ProxyInstallGuideResponse                  [DTO: HTML install guide]
│   ├── TerminalCommandRequest                     [DTO: terminal command]
│   └── ServerPortHolder                           [Utility: port configuration]
│
├── terminal/                                       [Interactive terminal (70%)]
│   ├── TerminalWebSocketHandler                   [WebSocket message routing]
│   ├── TerminalSessionManager                     [Session lifecycle]
│   ├── InteractiveTerminalSession                 [pty4j-based terminal]
│   ├── TerminalTicketService                      [One-time auth tickets]
│   ├── TerminalTicketController                   [REST: get ticket]
│   ├── TerminalMessage                            [DTO: WebSocket message]
│   ├── TerminalAuthHandshakeInterceptor           [WebSocket auth]
│   └── WebSocketConfig                            [WebSocket endpoint config]
│
├── db/                                             [Database layer]
│   ├── FileBasedHardeningOperationStateRepository
│   ├── FileBasedProxyOperationStateRepository
│   └── FileBasedGatewayProxyOperationStateRepository
│
└── web/                                            [SPA routing]
    └── SpaForwardController                       [Route unmatched URLs to index.html]
```

#### 3.2 Layer Breakdown by Responsibility

**REST Controllers (8 files):**
```
HardeningController              → /api/v1/hardening
GatewayProxyController           → /api/v1/gateway/proxy
ProxyController                  → /api/v1/proxy
ProxyInstallationController      → /api/v1/proxy/install
UserManagementController         → /api/v1/admin/users
AuthController                   → /api/v1/auth
TerminalTicketController         → /api/v1/terminal/ticket
SpaForwardController             → /* (Angular routing)
```

**Services (8 files):**
```
DefaultHardeningService          → Validates + orchestrates hardening
DefaultGatewayProxyService       → Validates + orchestrates nginx config
DefaultProxyService              → Proxy status detection
ProxyInstallationService         → Install guide generation
UserManagementService            → User CRUD + authorization
TerminalSessionManager           → Terminal session lifecycle
JsonUserDetailsService           → Spring Security integration
JsonUserStore                    → User persistence
```

**Adapters (6 files):**
```
LinuxHardeningAdapter            → Linux bash hardening scripts
WindowsHardeningAdapter          → Windows PowerShell hardening scripts
ProcessHardeningCommandExecutor  → ProcessBuilder for hardening
ProcessNginxCommandExecutor      → ProcessBuilder for nginx
InteractiveTerminalSession       → pty4j terminal emulation
```

**Repositories (3 files):**
```
FileBasedHardeningOperationStateRepository
FileBasedProxyOperationStateRepository
FileBasedGatewayProxyOperationStateRepository
```

**State Stores (4 files, memory + file delegation):**
```
InMemoryHardeningOperationStateStore        → Delegates to repository
InMemoryGatewayProxyOperationStateStore     → Delegates to repository
InMemoryProxyOperationStateStore            → Delegates to repository
```

---

## Dependency Graph

### Module Dependencies

```
GSServer-jar
    (no dependencies on other modules)
    └─ Spring Boot 3.3.2

GSServer-UI
    ├─ GSServer-db (file-based persistence)
    └─ Spring Boot 3.3.2
        ├─ Spring Security
        ├─ Spring Web
        ├─ Jackson (JSON)
        ├─ pty4j (terminal)
        └─ JUnit 5 (testing)

GSServer-db
    ├─ Jackson (JSON serialization)
    └─ Spring Core (minimal)
```

### Internal Component Dependencies (GSServer-UI)

**Controller → Service (direct dependencies):**
```
HardeningController
    → HardeningService (interface)
        → DefaultHardeningService (implementation)

GatewayProxyController
    → GatewayProxyService (interface)
        → DefaultGatewayProxyService (implementation)

ProxyController
    → ProxyService (interface)
        → DefaultProxyService (implementation)

UserManagementController
    → UserManagementService (concrete)

TerminalTicketController
    → TerminalTicketService (concrete)
```

**Service → Adapter (direct dependencies):**
```
DefaultHardeningService
    → LinuxHardeningAdapter
    → WindowsHardeningAdapter
    → HardeningCommandExecutor (interface)
        → ProcessHardeningCommandExecutor

DefaultGatewayProxyService
    → NginxCommandExecutor (interface)
        → ProcessNginxCommandExecutor
```

**Service → Repository (via State Store):**
```
DefaultHardeningService
    → HardeningOperationStateStore (interface)
        → InMemoryHardeningOperationStateStore
            → FileBasedHardeningOperationStateRepository
                → FileBasedOperationStateRepository<HardeningOperationState>
```

---

## Spring Boot Configuration

### Main Entry Point
```java
@SpringBootApplication
public class GsServerUiApplication {
  public static void main(String[] args) {
    SpringApplication.run(GsServerUiApplication.class, args);
  }
}
```

### Registered Components
- ✅ Controllers: Annotated with @RestController
- ✅ Services: Annotated with @Service
- ✅ Components: Adapters, stores (annotated @Component)
- ✅ Configuration: SecurityConfig, CorsConfig, WebSocketConfig
- ✅ Security: @EnableMethodSecurity for @PreAuthorize

### Configuration Properties
```properties
# application.properties
server.port=8080
server.servlet.context-path=/
logging.level.root=INFO
logging.level.com.gsserver=DEBUG

# Security (from SecurityUsersProperties)
app.security.thor-login-enabled=true (localhost only)
app.security.users-file=users.json
```

---

## Test Organization (12 test files)

```
src/test/java/com/gsserver/ui/

├── hardening/
│   ├── DefaultHardeningServiceTest           [Service + adapters]
│   ├── HardeningControllerContractTest       [REST contract validation]
│   └── adapter/
│       ├── ProcessHardeningCommandExecutorTest
│       ├── LinuxHardeningAdapterTest
│       └── WindowsHardeningAdapterTest

├── gateway/
│   ├── DefaultGatewayProxyServiceTest        [Service + executor]
│   └── GatewayProxyControllerContractTest    [REST contract]

├── proxy/
│   ├── DefaultProxyServiceTest
│   └── ProxyControllerContractTest

├── auth/
│   └── AuthControllerContractTest

├── db/
│   └── FileBasedHardeningOperationStateRepositoryTest

└── GsServerUiApplicationTests                [Spring context test]
```

---

## Build & Packaging

### Maven Build
```bash
mvn clean package

# Output:
# GSServer-pom/target/GSServer-pom.jar    (parent, doesn't produce jar)
# GSServer-db/target/GSServer-db-1.0.jar   (library jar)
# GSServer-jar/target/GSServer-jar-1.0.jar (standalone, not used)
# GSServer-UI/target/GSServer-UI-1.0.jar   (executable, embedded Tomcat + SPA)
```

### Runtime
```bash
java -jar GSServer-UI/target/GSServer-UI-1.0.jar
# Starts on http://localhost:8080
# Serves Angular SPA at /
# Serves REST API at /api/v1/*
# Serves WebSocket at /api/v1/terminal/ws
```

---

## Issues & Observations

### Code Organization Issues

| Issue | Impact | Fix |
|---|---|---|
| Concrete repos in UI instead of db | Violates module separation | Move to db module |
| GSServer-jar has no code | Dead module, confusing | Remove or wire properly |
| Terminal in proxy package | Semantic mismatch | Create terminal/ package |
| No common/ package for DTOs | DTOs duplicated conceptually | Consider shared DTOs |

### Dependency Issues

| Issue | Impact | Fix |
|---|---|---|
| Controllers name inconsistency | HardeningController vs GatewayProxyController | Rename to Handlers in Phase 2 |
| Service interfaces not always used | Proxy services are concrete | Use interfaces consistently |
| No adapter interface for terminal | Can't swap terminal implementations | Create TerminalAdapter |

---

## Summary

**GS-Server module structure (present):**
- **3 modules** with clean Maven organization
- **78 source files** (mostly in GSServer-UI)
- **Clean layer separation:** Controller → Service → Adapter → Repository
- **Multi-tenant ready:** Tenant ID threaded through all layers
- **File-based persistence:** JSON storage, full history, rollback support
- **GSServer-jar:** Exists but NOT integrated (Phase 2 task)

**Next phase improvements:**
1. Move concrete repositories to db module
2. Rename controllers to handlers (Phase 2)
3. Wire GSServer-jar as REST backend (Phase 1-2)
4. Establish service interfaces consistently
5. Consolidate DTO definitions

