# Naming Conventions

## Overview
All classes follow Spring's standard annotation pattern (@Controller, @Service, @Repository) with additional conventions for server infrastructure control.

---

## Handler vs Controller

### Handler (@Controller)
- **Purpose**: Orchestrates server infrastructure operations
- **Characteristics**: 
  - Controls hardening, proxy, firewall, or other server functionality
  - Thin orchestrator - delegates actual work to services/adapters
  - Entry point for server infrastructure requests
- **Examples**:
  - `HardeningHandler`
  - `GatewayProxyHandler`
  - `FirewallHandler`
  - `ProxyHandler`

### Controller (@Controller)
- **Purpose**: Handles regular HTTP/API operations (not server infrastructure)
- **Characteristics**:
  - Standard CRUD, data management, API endpoints
  - Does NOT control server infrastructure
  - Thin orchestrator - delegates to services
- **Examples**:
  - `UserController`
  - `ProductController`
  - `ReportController`
  - `AuthController`

---

## Naming Rules

### 1. Technology Abstraction
- **Rule**: Do NOT mention specific technologies in handler/controller names unless the component is ONLY usable for that technology
- **Examples**:
  - ✅ `GatewayProxyHandler` (abstracted, works for any proxy - nginx, Apache, Envoy)
  - ✅ `NginxConfigurationHandler` (OK - ONLY for nginx)
  - ✅ `HardeningHandler` (abstracted, works for Linux/Windows/macOS)
  - ❌ `ProxyServiceForNginx` (unnecessary tech reference)
  - ❌ `LinuxHardeningHandler` (should be abstracted)

### 2. Spring Annotation Standards

```java
// HTTP Entry Points - Controllers
@Controller
public class HardeningHandler { }

@Controller
public class UserController { }

// Business Logic & Orchestration - Services
@Service
public class DefaultHardeningHandler { }

@Service
public class UserService { }

// Data Persistence - Repositories
@Repository
public class HardeningRepository { }

@Repository
public class UserRepository { }
```

### 3. Adapters (No Handler Suffix)
- **Purpose**: Low-level implementations, OS-specific operations
- **Characteristics**: 
  - Execute actual system commands
  - Can mention specific technology/OS
  - NOT a Handler or Service
- **Examples**:
  - `LinuxHardeningAdapter`
  - `WindowsHardeningAdapter`
  - `ProcessNginxCommandAdapter`

---

## Architecture Layers

```
Handler (@Controller)
  ↓ delegates to
Service (@Service)
  ↓ delegates to
Adapter / Helper / Repository
  ↓ actually does work
```

### Layer Responsibilities

#### Handlers (@Controller)
- **Only**: Parse HTTP requests, validate format, route to service, return response
- **Forbidden**: Business logic, system operations, data manipulation
- **Thin orchestrator**: Nothing but delegation

#### Services (@Service)
- **Only**: Validate business rules, orchestrate adapters, coordinate operations
- **Forbidden**: Direct system operations, HTTP handling, data persistence (without repo)
- **Thin orchestrator**: Nothing but delegation and validation

#### Adapters
- **Only**: Execute system commands, perform actual operations
- **Forbidden**: HTTP handling, business logic orchestration
- **Actual work**: System calls, OS interactions

#### Repositories (@Repository)
- **Only**: Data persistence (CRUD with database/file system)
- **Forbidden**: Business logic, system operations
- **Data layer**: Abstraction for storage

---

## Golden Rules

1. **Handlers are thin orchestrators**: If a Handler contains more than routing/delegation logic, move it to Service
2. **Services are thin orchestrators**: If a Service contains more than validation/orchestration logic, move it to Adapter
3. **Only Adapters do work**: System commands, file operations, network calls happen in Adapters
4. **Handler = Server Infrastructure**: Only controllers that manage server operations are Handlers
5. **Technology abstraction**: Unless tech is the ONLY purpose, don't mention it in the name

---

## Naming Examples

### Good Examples ✅
- `HardeningHandler` + `DefaultHardeningService` + `LinuxHardeningAdapter`
- `GatewayProxyHandler` + `DefaultGatewayProxyService` + `ProcessNginxCommandAdapter`
- `FirewallHandler` + `DefaultFirewallService` + `IptablesAdapter` + `WindowsDefenderAdapter`
- `UserController` + `UserService` + `UserRepository`

### Bad Examples ❌
- `LinuxHardeningHandler` (mentions OS unnecessarily)
- `NginxProxyService` (mentions tech, should be abstracted)
- `HardeningController` (server infrastructure should use Handler)
- `UserHandler` (regular CRUD should use Controller, not Handler)
- `DefaultHardeningHandler` doing actual sysctl calls (work should be in Adapter)

---

## Refactoring Checklist

When reviewing code:
- [ ] Is it a @Controller? Is it server infrastructure? If yes → Handler. If no → Controller
- [ ] Does Handler contain only delegation? If not → move logic to Service
- [ ] Does Service contain only validation/orchestration? If not → move logic to Adapter
- [ ] Does class name mention unnecessary technology? If yes → remove it
- [ ] Are Adapters handling all actual system work? If not → move logic from Service/Handler

