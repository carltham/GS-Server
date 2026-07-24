# GS-Server Data Flow (Present)

**Project:** GS-Server  
**Version:** 1.0 (Present State)  
**Last Updated:** 2026-07-24  
**Scope:** Request/response flows for each capability

---

## 1. Hardening Flow

### Request
```
POST /api/v1/hardening
Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
Content-Type: application/json

{
  "tenantId": "tenant-a",
  "profile": "strict",
  "requestedBy": "ui-operator"
}
```

### Response (HTTP 202 Accepted)
```json
{
  "status": "accepted",
  "message": "Hardening operation started"
}
```

### Internal Flow

```
1. HTTP REQUEST
   POST /api/v1/hardening
   Authorization: Basic...

2. SPRING SECURITY
   ├─ SecurityFilterChain intercepts request
   ├─ LocalhostThorAuthenticationProvider checks if thor@localhost
   │   └─ YES: Grant passwordless access
   │   NO: JsonUserDetailsService loads user from JsonUserStore
   ├─ Compare password with bcrypt hash
   └─ Extract authorities (GROUP_HARDENING_OPERATORS, etc.)

3. ROUTING
   ├─ HardeningController.triggerHardening()
   └─ @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','...')")
      └─ Authority check: PASS or 403 Forbidden

4. REQUEST PARSING
   ├─ @RequestBody deserializes JSON → HardeningRequest
   ├─ Spring validates structure (types, required fields)
   └─ Pass to service

5. SERVICE LAYER: DefaultHardeningService.triggerHardening()
   ├─ validateRequest(request)
   │   ├─ Check: tenantId in ALLOWED_TENANTS
   │   ├─ Check: requestedBy in ALLOWED_OPERATORS
   │   ├─ Check: profile in ALLOWED_PROFILES
   │   └─ Throw PolicyViolationException if invalid
   │
   ├─ Call executeByProfile(request.profile())
   │   ├─ Detect platform: Linux or Windows
   │   │
   │   ├─ IF Linux:
   │   │   └─ LinuxHardeningAdapter.applyStrictHardening()
   │   │       ├─ Build bash script with Linux commands
   │   │       ├─ List<String> cmd = ["/usr/bin/env", "bash", "-lc", script]
   │   │       ├─ ProcessHardeningCommandExecutor.execute(cmd, 2-min timeout)
   │   │       │   ├─ ProcessBuilder pb = new ProcessBuilder(cmd)
   │   │       │   ├─ Process proc = pb.start()
   │   │       │   ├─ Read stdout, stderr from process streams
   │   │       │   ├─ Wait up to 2 minutes
   │   │       │   │   ├─ IF timeout: destroyForcibly(), return exit 124
   │   │       │   │   └─ ELSE: return proc.exitValue()
   │   │       │   └─ Return CommandExecutionResult(exitCode, stdout, stderr, timedOut)
   │   │       │
   │   │       └─ Return HardeningExecutionReport("linux", exitCode, stdout, stderr, timedOut)
   │   │
   │   └─ IF Windows:
   │       └─ WindowsHardeningAdapter.applyStrictHardening()
   │           ├─ Build PowerShell script with Windows commands
   │           └─ Similar ProcessBuilder flow
   │
   ├─ Check report.successful() (exitCode == 0)
   │   │
   │   ├─ IF SUCCESS:
   │   │   ├─ Create HardeningOperationState
   │   │   │   ├─ operationId: UUID.randomUUID()
   │   │   │   ├─ occurredAtUtc: Instant.now()
   │   │   │   ├─ status: "success"
   │   │   │   ├─ tenantId: request.tenantId()
   │   │   │   ├─ requestedBy: request.requestedBy()
   │   │   │   ├─ profile: request.profile()
   │   │   │   └─ ... (exitCode, timedOut, stderr)
   │   │   │
   │   │   └─ Persist via InMemoryHardeningOperationStateStore
   │   │       ├─ latest.set(state)  [memory]
   │   │       └─ repository.save(state)  [disk]
   │   │           ├─ ObjectMapper.writeValueAsString(state)
   │   │           └─ Files.writeString(data/hardening/operation-{id}.json)
   │   │
   │   └─ IF FAILURE:
   │       ├─ Call rollbackByProfile(request.profile())
   │       │   ├─ If baseline: revert kernel settings, disable UFW
   │       │   └─ If strict: revert all strict hardening
   │       ├─ Create HardeningOperationState
   │       │   ├─ status: "failed"
   │       │   ├─ rollbackStatus: "succeeded" or "failed"
   │       │   └─ stderr: error output
   │       ├─ Persist failed state to disk
   │       └─ Throw HardeningExecutionException(message)

6. CONTROLLER RESPONSE
   ├─ ResponseEntity.accepted() → HTTP 202
   ├─ Body: HardeningResponse("accepted", "Hardening started")
   └─ Return to client

7. STATE PERSISTED (async/background)
   └─ File exists: data/hardening/operation-{operationId}.json
      ├─ Ready for rollback
      ├─ Ready for audit logging (not yet)
      └─ Queryable via getLatest()
```

### State Model (Persisted JSON)

```json
{
  "operationId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAtUtc": "2026-07-24T10:00:00.000Z",
  "status": "success",
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "profile": "strict",
  "platform": "linux",
  "exitCode": 0,
  "timedOut": false,
  "rollbackStatus": "succeeded",
  "stderr": ""
}
```

### Error Cases

**Case 1: Invalid tenant**
```
Request: {"tenantId": "tenant-xyz", ...}
Service: validateTenantAccess() → throws PolicyViolationException
Response: HTTP 500 + ApiErrorResponse (NOT ideal - should be 422)
Details: "Invalid tenant" (no error code yet)
```

**Case 2: Execution timeout**
```
Adapter: ProcessBuilder runs > 2 minutes
Result: CommandExecutionResult(124, "", "", timedOut=true)
Report: HardeningExecutionReport(..., timedOut=true)
Service: report.successful() == false → triggers rollback
State: saved with timedOut=true, status="failed"
Response: HTTP 202 (operation persisted, but failed)
```

**Case 3: Rollback also fails**
```
Service: rollbackByProfile() returns unsuccessful report
State: saved with rollbackStatus="failed"
Exception: HardeningExecutionException("Hardening + rollback both failed")
Response: HTTP 500 (critical failure)
```

---

## 2. Gateway Proxy Flow

### Request (Recent Update: Now Executes Real Commands)
```
POST /api/v1/gateway/proxy/configure
Authorization: Basic...
Content-Type: application/json

{
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "enabled": true,
  "upstreamHost": "192.168.1.100",
  "upstreamPort": 8080,
  "tlsEnabled": false
}
```

### Response (HTTP 202 Accepted)
```json
{
  "status": "success",
  "message": "Nginx proxy configured successfully"
}
```

### Internal Flow

```
1. HTTP REQUEST
   POST /api/v1/gateway/proxy/configure

2. SPRING SECURITY
   ├─ Authentication (same as hardening)
   └─ @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS',...)")

3. ROUTING
   └─ GatewayProxyController.configureNginxProxy()

4. REQUEST PARSING
   └─ @RequestBody deserializes JSON → GatewayProxyRequest

5. SERVICE LAYER: DefaultGatewayProxyService.configureNginxProxy()
   ├─ validateRequest(request)
   │   ├─ Check: tenantId exists
   │   ├─ Check: requestedBy exists
   │   ├─ Check: upstreamHost is not blank
   │   └─ Check: upstreamPort in valid range (1-65535)
   │
   ├─ Build nginx configuration command
   │   ├─ NginxConfigurationCommand.build(request)
   │   ├─ Create nginx.conf snippet:
   │   │   upstream backend { server 192.168.1.100:8080; }
   │   │   server { ... proxy_pass http://backend; ... }
   │   └─ Validate nginx config
   │
   └─ Execute via NginxCommandExecutor
       └─ ProcessNginxCommandExecutor.execute()
           ├─ Run: nginx -t  (test config)
           │   ├─ IF success: exitCode 0
           │   └─ IF fail: exitCode 1, error in stderr
           │
           ├─ IF test passed:
           │   └─ Run: nginx -s reload  (apply config)
           │       ├─ IF success: config applied
           │       └─ IF fail: revert (atomic operation)
           │
           └─ Return NginxExecutionResult(success, message)

6. SERVICE CONTINUES
   ├─ Check result.successful()
   │
   ├─ IF SUCCESS:
   │   ├─ Create GatewayProxyOperationState
   │   │   ├─ operationId: UUID
   │   │   ├─ occurredAtUtc: Instant.now()
   │   │   ├─ status: "success"
   │   │   ├─ upstreamHost, upstreamPort (captured)
   │   │   └─ message: "Nginx configured successfully"
   │   │
   │   └─ Persist via InMemoryGatewayProxyOperationStateStore
   │       └─ Save to data/gateway-proxy/operation-{id}.json
   │
   └─ IF FAILED:
       ├─ Attempt rollback (restore prior nginx config)
       ├─ Create GatewayProxyOperationState with status="failed"
       └─ Throw GatewayProxyExecutionException

7. CONTROLLER RESPONSE
   ├─ ResponseEntity.accepted() → HTTP 202
   ├─ Body: GatewayProxyResponse("success", "Nginx configured...")
   └─ Return to client

8. NGINX STATE PERSISTED
   └─ File: data/gateway-proxy/operation-{operationId}.json
      ├─ Contains upstreamHost, upstreamPort (for rollback)
      └─ Can rollback to any prior operationId
```

### State Model (Persisted JSON)

```json
{
  "operationId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "occurredAtUtc": "2026-07-24T10:05:00.000Z",
  "status": "success",
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "enabled": true,
  "upstreamHost": "192.168.1.100",
  "upstreamPort": 8080,
  "tlsEnabled": false,
  "message": "Nginx proxy configured successfully"
}
```

### Rollback Flow

```
POST /api/v1/gateway/proxy/rollback/{operationId}

1. Load prior GatewayProxyOperationState from disk
   └─ File: data/gateway-proxy/operation-{operationId}.json

2. Extract upstream configuration
   └─ upstreamHost, upstreamPort

3. Rebuild nginx config with prior values
   └─ Same ProcessNginxCommandExecutor.execute()

4. Persist rollback state
   └─ New operationId, status="success" (or "failed" if rollback failed)

5. Return HTTP 202 Accepted
```

---

## 3. Proxy Management Flow (Status Detection)

### Request
```
GET /api/v1/proxy/status
Authorization: Basic...
```

### Response
```json
{
  "nginxRunning": true,
  "apacheRunning": false,
  "detectedServer": "nginx"
}
```

### Internal Flow

```
1. HTTP REQUEST
   GET /api/v1/proxy/status

2. ROUTING
   └─ ProxyController.getProxyStatus()

3. SERVICE LAYER: DefaultProxyService.getRuntimeStatus()
   ├─ Check if nginx process running
   │   └─ System.getRuntime().exec("pgrep nginx")
   │
   ├─ Check if Apache running
   │   ├─ System.getRuntime().exec("pgrep apache2")
   │   └─ System.getRuntime().exec("pgrep httpd")
   │
   └─ Return ProxyRuntimeStatus(nginxRunning, apacheRunning, detectedServer)

4. CONTROLLER RESPONSE
   └─ ResponseEntity.ok(status)
```

**Current limitation:** Runtime detection only (no actual nginx commands, just process checks).

---

## 4. User Management Flow

### Create User Request
```
POST /api/v1/admin/users
Authorization: Basic admin:password
Content-Type: application/json

{
  "username": "new-operator",
  "password": "secure-password-123",
  "authorities": ["GROUP_HARDENING_OPERATORS"]
}
```

### Response
```json
{
  "username": "new-operator",
  "authorities": ["GROUP_HARDENING_OPERATORS"],
  "enabled": true
}
```

### Internal Flow

```
1. HTTP REQUEST + SECURITY
   ├─ @PreAuthorize requires GROUP_HARDENING_ADMINS or GROUP_SUPERUSER
   └─ Only admins+ can create users

2. REQUEST PARSING
   └─ @RequestBody → CreateUserRequest

3. SERVICE: UserManagementService.create()
   ├─ Validate username format (alphanumeric, 2-32 chars)
   ├─ Check username doesn't already exist
   │   └─ JsonUserStore.exists(username)
   │
   ├─ Normalize authorities (filter to known roles)
   ├─ Check caller has permission to grant roles
   │   ├─ Admin cannot grant GROUP_SUPERUSER
   │   ├─ Admin cannot grant GROUP_HARDENING_ADMINS
   │   └─ Superadmin can grant any role
   │
   ├─ Encode password with bcrypt
   │   └─ PasswordEncoder.encode(password)
   │
   └─ Persist user
       └─ JsonUserStore.save(new ManagedUser(username, encodedPassword, authorities))
           ├─ ObjectMapper.writeValueAsString(users)
           └─ Files.writeString(data/users/users.json)

4. CONTROLLER RESPONSE
   └─ ResponseEntity.ok(UserSummary(...))
```

### State Model (Persisted JSON: users.json)

```json
[
  {
    "username": "ui-operator",
    "password": "{bcrypt}$2a$10$abcdef...",
    "authorities": ["GROUP_HARDENING_OPERATORS"],
    "enabled": true
  },
  {
    "username": "new-operator",
    "password": "{bcrypt}$2a$10$ghijkl...",
    "authorities": ["GROUP_HARDENING_OPERATORS"],
    "enabled": true
  }
]
```

---

## 5. Terminal Access Flow

### Step 1: Get One-Time Ticket

```
POST /api/v1/terminal/ticket
Authorization: Basic...
```

Response:
```json
{
  "ticket": "one-time-token-uuid-12345"
}
```

### Step 2: Connect via WebSocket

```
GET /api/v1/terminal/ws?ticket=one-time-token-uuid-12345
Upgrade: websocket
```

### Internal Flow

```
1. REST REQUEST
   POST /api/v1/terminal/ticket
   └─ TerminalTicketController.getTicket()
       ├─ Authentication required (user logged in)
       ├─ Generate one-time ticket
       │   └─ TerminalTicketService.generateTicket()
       │       ├─ UUID.randomUUID()
       │       └─ Store in-memory with 5-min expiry
       │
       └─ Return ticket to client

2. WEBSOCKET CONNECTION
   GET /api/v1/terminal/ws?ticket=one-time-token-uuid-12345
   └─ TerminalAuthHandshakeInterceptor.beforeHandshake()
       ├─ Extract ticket from query params
       ├─ Validate ticket exists and not expired
       │   └─ TerminalTicketService.validateAndConsume(ticket)
       │
       ├─ IF valid: Allow handshake
       └─ IF invalid: Reject with 403

3. WEBSOCKET ESTABLISHED
   └─ TerminalWebSocketHandler.afterConnectionEstablished()
       ├─ Create new session
       └─ TerminalSessionManager.createSession()
           ├─ PtyProcess process = pty4j.spawnProcess(cmd)
           └─ Store in sessionMap

4. USER INPUT
   User types in terminal UI
       ↓
   WebSocket frame: {"type": "input", "data": "ls -la\n"}
       ↓
   TerminalWebSocketHandler.handleTextMessage()
       ├─ Parse message
       ├─ Lookup session by connectionId
       └─ InteractiveTerminalSession.sendInput(text)
           └─ Write to pty4j process stdin

5. PROCESS OUTPUT
   pty4j reads from process
       ↓
   InteractiveTerminalSession polls output
       ↓
   TerminalWebSocketHandler sends to client
       ↓
   WebSocket frame: {"type": "output", "data": "total 42\ndrwxr..."}
       ↓
   Terminal UI displays

6. DISCONNECTION
   User closes terminal
       ↓
   TerminalWebSocketHandler.afterConnectionClosed()
       ├─ Lookup session
       └─ InteractiveTerminalSession.close()
           ├─ Process.destroy()
           └─ Remove from sessionMap
```

### State Model (In-Memory Only)

```
InteractiveTerminalSession:
{
  sessionId: "uuid-12345",
  process: PtyProcess,
  stdin: ProcessBuilder.start() input stream,
  stdout: ProcessBuilder.start() output stream,
  isRunning: true
}
```

---

## 6. Authentication Flow

### Login (HTTP Basic)

```
GET /api/v1/hardening/latest
Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
```

### Internal Flow

```
1. HTTP REQUEST
   Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
       ├─ Decode: username:password
       └─ username="ui-operator", password="secure-pass-123"

2. SPRING SECURITY
   └─ SecurityFilterChain.doFilter()
       ├─ Check if request is localhost
       │   └─ RequestOriginUtils.isLocalhost()
       │
       ├─ IF localhost AND username == "thor":
       │   └─ LocalhostThorAuthenticationProvider
       │       ├─ Allow passwordless (any password accepted)
       │       └─ Create Authentication with authorities
       │
       └─ ELSE (any other user):
           └─ JsonUserDetailsService
               ├─ Load user from JsonUserStore
               │   └─ JsonUserStore.findByUsername(username)
               │
               ├─ Compare password with bcrypt hash
               │   └─ PasswordEncoder.matches(password, storedHash)
               │
               └─ IF match: Create Authentication with user's authorities
                  IF no match: Reject with 401

3. @PreAuthorize CHECK
   └─ If endpoint has @PreAuthorize("hasAuthority('GROUP_HARDENING_OPERATORS')")
       ├─ Extract user authorities from Authentication
       ├─ Check if GROUP_HARDENING_OPERATORS in authorities
       └─ IF yes: Allow, IF no: 403 Forbidden

4. ENDPOINT EXECUTION
   └─ Controller method executes with authenticated user context
```

### Passwordless Thor (Localhost Only)

```
Requirements:
1. Request origin must be localhost (127.0.0.1 or ::1)
2. Username must be "thor"
3. Any password accepted (ignored)
4. Grant authorities from JSON store

Example:
  curl -u thor:anything http://localhost:8080/api/v1/hardening/latest
  ✅ Works (localhost, username=thor, password=ignored)

  curl -u thor:anything http://10.0.0.1:8080/api/v1/hardening/latest
  ❌ Fails (not localhost)
```

---

## 7. Error Handling Flow

### Error Path

```
1. Exception thrown anywhere
   ├─ PolicyViolationException (validation failure)
   ├─ HardeningExecutionException (operation failure)
   ├─ GatewayProxyExecutionException (proxy failure)
   └─ Any unhandled exception

2. ApiExceptionHandler (global @RestControllerAdvice)
   ├─ Catch exception
   └─ Build ApiErrorResponse
       ├─ Map exception to HTTP status
       │   ├─ PolicyViolationException → 500 (should be 422)
       │   ├─ HardeningExecutionException → 500
       │   └─ Unknown → 500
       │
       └─ Return error JSON

3. Response (HTTP 500)
   ```json
   {
     "timestamp": "2026-07-24T10:00:00.000Z",
     "status": 500,
     "error": "Internal Server Error",
     "message": "Hardening execution failed: ..."
   }
   ```

Current Issues:
- ❌ No errorId (can't track specific errors)
- ❌ No error code (generic "Internal Server Error")
- ❌ Secrets may leak in message (e.g., "Failed at /root/.ssh/id_rsa")
- ❌ No retry information (errorId, retryable field)
- ⚠️ All validation errors mapped to 500 (should be 422)
```

---

## Summary

**Data flows implement:**
- ✅ Clean separation: Controller → Service → Adapter → Repository
- ✅ Validation layers: Format (Spring) + Business (Service)
- ✅ Real execution: ProcessBuilder for hardening, nginx, terminal
- ✅ State persistence: JSON files, full history, rollback ready
- ✅ Authentication: HTTP Basic + passwordless thor
- ✅ Authorization: @PreAuthorize + tenant validation

**Gaps:**
- ❌ Secrets redaction (no filtering in error messages)
- ❌ Audit logging (no operation trail with actor/timestamp)
- ❌ Structured errors (no errorId, code, or details)
- ❌ Correlation IDs (can't trace across services)
- ❌ Structured logging (no tenant/correlation context)

