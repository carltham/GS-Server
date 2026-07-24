# GS-Server Component Map (Present)

**Project:** GS-Server  
**Version:** 1.0 (Present State)  
**Last Updated:** 2026-07-24  
**Scope:** 78 source files, organized by capability + layer

---

## Component Organization

All 78 components organized by **capability** (what it does) and **layer** (responsibility).

---

## 1. HARDENING (20 components, 95% complete)

### 1.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningController` | @RestController | Route /api/v1/hardening requests | ✅ Implemented |

### 1.2 Service Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningService` | Interface | Define hardening contract | ✅ Implemented |
| `DefaultHardeningService` | @Service | Orchestrate hardening operations | ✅ Implemented |

### 1.3 Adapter Layer (Platform-Specific Execution)
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningCommandExecutor` | Interface | Define command execution contract | ✅ Implemented |
| `ProcessHardeningCommandExecutor` | @Component | Execute shell commands via ProcessBuilder | ✅ Implemented |
| `LinuxHardeningAdapter` | @Component | Linux-specific hardening (bash scripts) | ✅ Implemented |
| `WindowsHardeningAdapter` | @Component | Windows-specific hardening (PowerShell) | ✅ Implemented |

### 1.4 Repository/Persistence Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningOperationStateStore` | Interface | Define state store contract | ✅ Implemented |
| `InMemoryHardeningOperationStateStore` | @Component | Memory cache + file delegation | ✅ Implemented |
| `FileBasedHardeningOperationStateRepository` | @Component | JSON persistence for hardening | ✅ Implemented |

### 1.5 Data Models (Domain Objects)
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningOperationState` | Record | Domain: hardening operation state | ✅ Implemented |
| `HardeningRequest` | Record | DTO: API request | ✅ Implemented |
| `HardeningResponse` | Record | DTO: API response (202 Accepted) | ✅ Implemented |
| `CommandExecutionResult` | Record | DTO: process execution result | ✅ Implemented |
| `HardeningExecutionReport` | Record | DTO: hardening operation result | ✅ Implemented |

### 1.6 Exception Handling
| Component | Type | Purpose | Status |
|---|---|---|---|
| `HardeningExecutionException` | Exception | Execution failure (non-recoverable) | ✅ Implemented |
| `PolicyViolationException` | Exception | Validation failure (recoverable) | ✅ Implemented |

---

## 2. GATEWAY PROXY (16 components, 50% complete)

### 2.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GatewayProxyController` | @RestController | Route /api/v1/gateway/proxy requests | ✅ Implemented |

### 2.2 Service Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GatewayProxyService` | Interface | Define gateway proxy contract | ✅ Implemented |
| `DefaultGatewayProxyService` | @Service | Configure nginx, manage rollback | ✅ Implemented |

### 2.3 Adapter Layer (Nginx Execution)
| Component | Type | Purpose | Status |
|---|---|---|---|
| `NginxCommandExecutor` | Interface | Define nginx command execution | ✅ Implemented |
| `ProcessNginxCommandExecutor` | @Component | Execute nginx commands via ProcessBuilder | ✅ Implemented |
| `NginxConfigurationCommand` | Record | Build nginx configuration commands | ✅ Implemented |

### 2.4 Repository/Persistence Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GatewayProxyOperationStateStore` | Interface | Define state store contract | ✅ Implemented |
| `InMemoryGatewayProxyOperationStateStore` | @Component | Memory cache + file delegation | ✅ Implemented |
| `FileBasedGatewayProxyOperationStateRepository` | @Component | JSON persistence for proxy | ✅ Implemented |

### 2.5 Data Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GatewayProxyOperationState` | Record | Domain: proxy operation state | ✅ Implemented |
| `GatewayProxyRequest` | Record | DTO: configure proxy request | ✅ Implemented |
| `GatewayProxyResponse` | Record | DTO: API response (202 Accepted) | ✅ Implemented |
| `NginxExecutionResult` | Record | DTO: nginx command result | ✅ Implemented |

### 2.6 Exception Handling
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GatewayProxyExecutionException` | Exception | Proxy execution failure | ✅ Implemented |

---

## 3. PROXY MANAGEMENT (14 components, 50% complete)

### 3.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ProxyController` | @RestController | Route /api/v1/proxy requests | ✅ Implemented |
| `ProxyInstallationController` | @RestController | Route /api/v1/proxy/install requests | ✅ Implemented |

### 3.2 Service Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ProxyService` | Interface | Define proxy management contract | ✅ Implemented |
| `DefaultProxyService` | @Service | Status detection (no real commands) | ⚠️ Stub |
| `ProxyInstallationService` | Concrete @Service | Generate installation guide HTML | ✅ Implemented |

### 3.3 Repository/Persistence Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ProxyOperationStateStore` | Interface | Define state store contract | ✅ Implemented |
| `InMemoryProxyOperationStateStore` | @Component | Memory cache + file delegation | ✅ Implemented |
| `FileBasedProxyOperationStateRepository` | @Component | JSON persistence for proxy ops | ✅ Implemented |

### 3.4 Data Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ProxyOperationState` | Record | Domain: proxy operation state | ✅ Implemented |
| `ProxyRequest` | Record | DTO: apply proxy config request | ✅ Implemented |
| `ProxyResponse` | Record | DTO: API response | ✅ Implemented |
| `ProxyRuntimeStatus` | Record | DTO: nginx/apache status | ✅ Implemented |
| `ProxyInstallGuideResponse` | Record | DTO: HTML install guide | ✅ Implemented |
| `SiteFileResponse` | Record | DTO: site file | ✅ Implemented |
| `ServerPortHolder` | Utility | Port configuration | ✅ Implemented |
| `TerminalCommandRequest` | Record | DTO: terminal command | ✅ Implemented |

---

## 4. USER MANAGEMENT (10 components, 90% complete)

### 4.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `UserManagementController` | @RestController | Route /api/v1/admin/users requests | ✅ Implemented |

### 4.2 Service Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `UserManagementService` | Concrete @Service | CRUD + RBAC (no interface) | ✅ Implemented |

### 4.3 Security/Storage Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `JsonUserStore` | Concrete | User persistence (file-based JSON) | ✅ Implemented |

### 4.4 Data Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ManagedUser` | Record | Domain: user entity with roles | ✅ Implemented |
| `UserSummary` | Record | DTO: user list response | ✅ Implemented |
| `CreateUserRequest` | Record | DTO: create user request | ✅ Implemented |
| `UpdateUserRequest` | Record | DTO: update user request | ✅ Implemented |
| `PasswordResetRequest` | Record | DTO: password reset request | ✅ Implemented |

---

## 5. AUTHENTICATION & SECURITY (12 components, 85% complete)

### 5.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `AuthController` | @RestController | Route /api/v1/auth/config (public) | ✅ Implemented |

### 5.2 Security Configuration
| Component | Type | Purpose | Status |
|---|---|---|---|
| `SecurityConfig` | @Configuration | Spring Security setup (@PreAuthorize) | ✅ Implemented |
| `LocalhostThorAuthenticationProvider` | AuthenticationProvider | Passwordless thor (localhost only) | ✅ Implemented |
| `JsonUserDetailsService` | UserDetailsService | Load users from JSON store | ✅ Implemented |

### 5.3 Security Utilities
| Component | Type | Purpose | Status |
|---|---|---|---|
| `RequestOriginUtils` | Utility | Detect localhost origin | ✅ Implemented |
| `CorsConfig` | @Configuration | CORS headers for Angular | ✅ Implemented |
| `SecurityUsersProperties` | @ConfigurationProperties | Security configuration | ✅ Implemented |

### 5.4 Data Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `AuthIdentityResponse` | Record | DTO: auth config response | ✅ Implemented |

---

## 6. TERMINAL ACCESS (10 components, 70% complete)

### 6.1 HTTP/REST Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `TerminalTicketController` | @RestController | Route /api/v1/terminal/ticket | ✅ Implemented |

### 6.2 WebSocket Layer
| Component | Type | Purpose | Status |
|---|---|---|---|
| `WebSocketConfig` | @Configuration | Register /api/v1/terminal/ws endpoint | ✅ Implemented |
| `TerminalWebSocketHandler` | WebSocketHandler | WebSocket message routing | ✅ Implemented |
| `TerminalAuthHandshakeInterceptor` | HandshakeInterceptor | Validate one-time ticket at handshake | ✅ Implemented |

### 6.3 Service/Session Management
| Component | Type | Purpose | Status |
|---|---|---|---|
| `TerminalSessionManager` | Concrete | Create/manage/close sessions | ✅ Implemented |
| `TerminalTicketService` | Concrete | Generate one-time auth tickets | ✅ Implemented |
| `InteractiveTerminalSession` | Concrete | pty4j-based terminal emulation | ✅ Implemented |

### 6.4 Data Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `TerminalMessage` | Record | DTO: WebSocket message | ✅ Implemented |

---

## 7. ERROR HANDLING & API (3 components)

### 7.1 Global Exception Handling
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ApiExceptionHandler` | @RestControllerAdvice | Catch exceptions, return 500 JSON | ✅ Implemented |

### 7.2 Error Response Models
| Component | Type | Purpose | Status |
|---|---|---|---|
| `ApiErrorResponse` | Record | DTO: error response structure | ✅ Implemented |

### 7.3 SPA Routing
| Component | Type | Purpose | Status |
|---|---|---|---|
| `SpaForwardController` | @RestController | Route unmatched URLs to index.html | ✅ Implemented |

---

## 8. PERSISTENCE (1 component, in db module)

### 8.1 Abstract Base Repository
| Component | Type | Purpose | Status |
|---|---|---|---|
| `OperationStateRepository<T>` | Interface | Generic persistence contract | ✅ Implemented |
| `FileBasedOperationStateRepository<T>` | Abstract | Generic JSON file I/O | ✅ Implemented |

---

## 9. APPLICATIONS & BOOTSTRAP (2 components)

### 9.1 Spring Boot Entry Points
| Component | Type | Purpose | Status |
|---|---|---|---|
| `GsServerUiApplication` | @SpringBootApplication | GSServer-UI main class (RUNS) | ✅ Implemented |
| `GsServerJarApplication` | @SpringBootApplication | GSServer-jar main class (NOT WIRED) | ⚠️ Stub |

---

## Component Count Summary

| Capability | Controllers | Services | Adapters | Repositories | Data Models | Exceptions | Utilities | **Total** |
|---|---|---|---|---|---|---|---|---|
| **Hardening** | 1 | 2 | 4 | 2 | 5 | 2 | - | **16** |
| **Gateway Proxy** | 1 | 2 | 3 | 2 | 4 | 1 | - | **13** |
| **Proxy Mgmt** | 2 | 3 | - | 2 | 8 | - | 1 | **16** |
| **User Mgmt** | 1 | 1 | - | 1 | 5 | - | - | **8** |
| **Auth & Security** | 1 | - | - | - | 1 | - | 3 | **5** |
| **Terminal** | 1 | 3 | - | - | 1 | - | - | **5** |
| **API & Error** | 1 | - | - | - | 1 | - | - | **2** |
| **Persistence** | - | - | - | 2 | - | - | - | **2** |
| **Bootstrap** | - | - | - | - | - | - | 2 | **2** |
| **TOTAL** | **9** | **11** | **7** | **9** | **25** | **3** | **4** | **78** |

---

## Component Dependency Graph

### Critical Path (Hardening Request)

```
HardeningController (HTTP layer)
    ↓ calls
HardeningService (business logic)
    ├─ calls ProcessHardeningCommandExecutor
    ├─ calls LinuxHardeningAdapter or WindowsHardeningAdapter
    └─ calls HardeningOperationStateStore
            ↓ delegates to
        FileBasedHardeningOperationStateRepository
            ↓ uses
        FileBasedOperationStateRepository (abstract)
```

### Critical Path (Proxy Request)

```
GatewayProxyController (HTTP layer)
    ↓ calls
GatewayProxyService (business logic)
    ├─ calls ProcessNginxCommandExecutor
    └─ calls GatewayProxyOperationStateStore
            ↓ delegates to
        FileBasedGatewayProxyOperationStateRepository
            ↓ uses
        FileBasedOperationStateRepository (abstract)
```

### Security Path (Every Request)

```
HTTP Request
    ↓
SecurityFilterChain (Spring Security)
    ├─ CORS headers (CorsConfig)
    ├─ Authentication
    │   └─ LocalhostThorAuthenticationProvider
    │       or JsonUserDetailsService
    │           └─ JsonUserStore
    │
    └─ @PreAuthorize annotation on controller
```

---

## Missing Components (Gaps vs Target)

### Critical Gaps
- ❌ Audit logging component (no operation trail with actor/timestamp)
- ❌ Secrets redaction component (credentials leak in errors)
- ❌ Structured error model (no errorId/code/details)
- ❌ Correlation ID component (can't trace across services)

### High-Priority Gaps
- ❌ Apache proxy adapter (only nginx)
- ❌ Firewall adapter (not implemented)
- ❌ Cross-tenant tests (authorization not verified)

### Medium-Priority Gaps
- ❌ Metrics collection (no operation success rate)
- ❌ Structured logging (no tenant/correlation context)
- ❌ OAuth2 authentication provider
- ❌ API specification (no OpenAPI/Swagger)

---

## Component Reusability

### Patterns Used

**Adapter Pattern** (multiple implementations, single interface):
- HardeningAdapter: LinuxHardeningAdapter, WindowsHardeningAdapter
- NginxCommandExecutor: ProcessNginxCommandExecutor, (future: RemoteNginxExecutor)

**State Store Pattern** (memory + delegation):
- InMemoryHardeningOperationStateStore delegates to FileBasedHardeningOperationStateRepository
- InMemoryGatewayProxyOperationStateStore delegates to FileBasedGatewayProxyOperationStateRepository

**Generic Repository Pattern** (T-parameterized):
- FileBasedOperationStateRepository<T> (abstract)
- FileBasedHardeningOperationStateRepository extends FileBasedOperationStateRepository<HardeningOperationState>
- FileBasedProxyOperationStateRepository extends FileBasedOperationStateRepository<ProxyOperationState>

**Service Interface Pattern** (contract-based):
- HardeningService interface, DefaultHardeningService implementation
- GatewayProxyService interface, DefaultGatewayProxyService implementation
- ProxyService interface, DefaultProxyService implementation

---

## Summary

**78 components organized by:**
- **Capability:** Hardening, Proxy, User Mgmt, Security, Terminal
- **Layer:** Controllers (9), Services (11), Adapters (7), Repositories (9), Data Models (25), Exceptions (3), Utilities (4)

**Status:**
- ✅ Hardening: 95% complete (16 components)
- ✅ Gateway Proxy: 50% complete (13 components, real nginx commands wired)
- ✅ Proxy Management: 50% complete (16 components, status detection only)
- ✅ User Management: 90% complete (8 components)
- ✅ Security: 85% complete (5 components, no OAuth2 yet)
- ✅ Terminal: 70% complete (5 components, audit logging missing)

**Critical missing components:** Audit logging, secrets redaction, structured errors, correlation IDs.

