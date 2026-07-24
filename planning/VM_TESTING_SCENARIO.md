# VM-Based Development & Testing Scenario

## Overview

Comprehensive VM-based infrastructure for safe development, testing, and validation of hardening operations before production deployment. Isolates security-sensitive operations and enables realistic end-to-end testing.

---

## Architecture: Multi-VM Testing Environment

```
┌─────────────────────────────────────────────────────────────────┐
│                     Host Machine (Your System)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Hypervisor (KVM/VirtualBox/Hyper-V)         │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                            │   │
│  │  ┌─────────────────┐  ┌─────────────────┐               │   │
│  │  │  DEV-BASE VM    │  │  TEST-BASELINE  │               │   │
│  │  │  (Ubuntu 22.04) │  │  (Ubuntu 22.04) │               │   │
│  │  │  - Spring Boot  │  │  - Baseline OS  │               │   │
│  │  │  - Maven        │  │  - Hardening    │               │   │
│  │  │  - Postgres     │  │    disabled     │               │   │
│  │  │  - Port 8080    │  │  - Port 8081    │               │   │
│  │  └─────────────────┘  └─────────────────┘               │   │
│  │           ▲                     ▲                        │   │
│  │           │                     │                        │   │
│  │  ┌─────────────────┐  ┌─────────────────┐               │   │
│  │  │ TEST-HARDENED  │  │  TEST-ISOLATED  │               │   │
│  │  │ (Ubuntu 22.04) │  │ (Ubuntu 22.04)  │               │   │
│  │  │ - Hardening    │  │ - Cross-tenant  │               │   │
│  │  │   applied      │  │   isolation     │               │   │
│  │  │ - Port 8082    │  │ - Port 8083     │               │   │
│  │  └─────────────────┘  └─────────────────┘               │   │
│  │           ▲                     ▲                        │   │
│  │           │                     │                        │   │
│  │  ┌──────────────────────────────────────────┐            │   │
│  │  │      Internal Network (192.168.X.X/24)  │            │   │
│  │  │  - All VMs on shared internal network    │            │   │
│  │  │  - No external internet access          │            │   │
│  │  │  - Controlled cross-VM communication    │            │   │
│  │  └──────────────────────────────────────────┘            │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. VM Specifications

### DEV-BASE VM (Development)
**Purpose:** Primary development environment for application code changes

```
OS:           Ubuntu 22.04 LTS
CPU:          4 cores
RAM:          8 GB
Storage:      50 GB
Network:      Internal network (192.168.100.10)
```

**Pre-installed:**
- OpenJDK 21
- Maven 3.9.x
- PostgreSQL 15
- Git
- IntelliJ IDEA / VS Code (optional)
- Docker (for artifact building)

**Ports:**
- `8080`: Spring Boot application
- `5432`: PostgreSQL database

**Purpose of this VM:**
- Run `mvn clean install`
- Run integration tests
- Debug application logic
- Build JAR artifacts

---

### TEST-BASELINE VM
**Purpose:** Baseline system state before hardening (control group)

```
OS:           Ubuntu 22.04 LTS (fresh install)
CPU:          4 cores
RAM:          8 GB
Storage:      50 GB
Network:      Internal network (192.168.100.11)
Snapshot:     BEFORE hardening applied
```

**Configuration:**
- Fresh OS with standard security
- No hardening operations applied
- PostgreSQL configured with test data
- Spring Boot application deployed (JAR)
- Pre-hardening security baseline recorded

**Ports:**
- `8081`: Spring Boot application
- `5432`: PostgreSQL database

**Tests to run:**
1. Application startup & health checks
2. API endpoint functionality (no hardening effects)
3. Baseline performance metrics
4. Baseline security audit (open ports, running services)

---

### TEST-HARDENED VM
**Purpose:** System state after hardening operations (test group)

```
OS:           Ubuntu 22.04 LTS (cloned from BASELINE)
CPU:          4 cores
RAM:          8 GB
Storage:      50 GB
Network:      Internal network (192.168.100.12)
Snapshot:     AFTER hardening applied
```

**Configuration:**
- Cloned from TEST-BASELINE
- Hardening profile applied (via running application)
- PostgreSQL configured with test data
- Spring Boot application deployed (JAR)

**Ports:**
- `8082`: Spring Boot application
- `5432`: PostgreSQL database

**Tests to run:**
1. Application functionality after hardening
2. Security improvements verified (firewall rules, SELinux, etc.)
3. Performance metrics post-hardening
4. Security audit (closed ports, restricted services)
5. Regression tests (nothing broken by hardening)

---

### TEST-ISOLATED VM
**Purpose:** Cross-tenant isolation & multi-tenant security validation

```
OS:           Ubuntu 22.04 LTS (fresh install)
CPU:          4 cores
RAM:          8 GB
Storage:      50 GB
Network:      Internal network (192.168.100.13)
```

**Configuration:**
- Two separate Spring Boot applications (tenant-a, tenant-b)
- Each with isolated database
- Network firewall rules prevent cross-tenant access

**Ports:**
- `8083`: Tenant-A application
- `8084`: Tenant-B application
- `5432`: Tenant-A database
- `5433`: Tenant-B database

**Tests to run:**
1. Tenant-A cannot access Tenant-B resources
2. Tenant-A cannot trigger Tenant-B hardening
3. Audit logs separate by tenant
4. Correlation IDs don't leak across tenants

---

## 2. Development Workflow

### Phase 1: Setup (One-time)

```bash
# 1. Install hypervisor
sudo apt install qemu-kvm libvirt-daemon-system libvirt-clients

# 2. Create VM network
virsh net-create vm-network.xml

# 3. Create base VM image
# - Download Ubuntu 22.04 ISO
# - Install to 50GB disk
# - Run initial setup script

# 4. Clone base image to create TEST-BASELINE, TEST-HARDENED, TEST-ISOLATED
# - Each gets own snapshot
# - Each gets own network interface (192.168.100.10-13)

# 5. Configure each VM
# - Update /etc/hostname
# - Configure database
# - Deploy application JAR
```

### Phase 2: Continuous Development Cycle

```
1. Developer makes changes in DEV-BASE VM
   └─ Run: mvn clean install -DskipTests
   └─ Run: mvn test (unit tests)
   └─ Run: mvn integration-test (focused ITs)

2. Build application artifact (JAR)
   └─ Run: mvn clean package

3. Deploy to TEST-BASELINE VM
   └─ Run: java -jar application.jar
   └─ Run: baseline health checks
   └─ Record: baseline metrics

4. Deploy to TEST-HARDENED VM
   └─ Run: java -jar application.jar
   └─ Run: POST /api/v1/hardening (trigger hardening via app)
   └─ Record: post-hardening metrics
   └─ Verify: hardening applied correctly

5. Compare: BASELINE vs HARDENED
   └─ Performance regression?
   └─ Security improvements?
   └─ Any broken functionality?

6. Run cross-tenant tests on TEST-ISOLATED VM
   └─ Verify isolation enforced
   └─ Verify audit logging per tenant
```

---

## 3. Testing Scenarios

### Scenario A: Baseline Functionality
**Location:** TEST-BASELINE VM

```bash
# 1. Start application
ssh test-baseline "sudo systemctl start gsserver"

# 2. Health check
curl http://192.168.100.11:8081/api/v1/health

# 3. API functionality
curl -X POST http://192.168.100.11:8081/api/v1/hardening \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenant-a","requestedBy":"ui-operator","profile":"baseline"}'

# 4. Verify no hardening applied
ssh test-baseline "sudo iptables -L | grep -i DROP"  # Should be minimal
ssh test-baseline "sudo ss -tuln"  # Should show many open ports
```

### Scenario B: Hardening Validation
**Location:** TEST-HARDENED VM

```bash
# 1. Clone from BASELINE
virsh clone-volume test-baseline test-hardened

# 2. Start application
ssh test-hardened "sudo systemctl start gsserver"

# 3. Trigger hardening via API
curl -X POST http://192.168.100.12:8082/api/v1/hardening \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenant-a","requestedBy":"ui-operator","profile":"strict"}'

# 4. Verify hardening applied
ssh test-hardened "sudo iptables -L | grep -i DROP"   # Should be many
ssh test-hardened "sudo ss -tuln"                      # Should be restricted
ssh test-hardened "sudo getenforce"                    # SELinux enforcing?

# 5. Verify application still works
curl http://192.168.100.12:8082/api/v1/health

# 6. Performance comparison
# Record metrics: response times, throughput, resource usage
```

### Scenario C: Cross-Tenant Isolation
**Location:** TEST-ISOLATED VM

```bash
# 1. Start Tenant-A application
ssh test-isolated "ssh 192.168.100.13 sudo systemctl start gsserver-tenant-a"

# 2. Start Tenant-B application
ssh test-isolated "ssh 192.168.100.13 sudo systemctl start gsserver-tenant-b"

# 3. Tenant-A tries to trigger hardening on its own resources
curl -X POST http://192.168.100.13:8083/api/v1/hardening \
  -H "Authorization: Bearer tenant-a-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenant-a","requestedBy":"ui-operator","profile":"strict"}'
# Expected: SUCCESS (202 Accepted)

# 4. Tenant-A tries to trigger hardening on Tenant-B resources
curl -X POST http://192.168.100.13:8083/api/v1/hardening \
  -H "Authorization: Bearer tenant-a-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenant-b","requestedBy":"ui-operator","profile":"strict"}'
# Expected: DENIED (403 Forbidden)

# 5. Verify Tenant-B hardening did NOT apply
ssh test-isolated "ssh 192.168.100.14 sudo iptables -L | grep -i DROP"
# Expected: Same as before (no hardening)

# 6. Check audit logs are separated by tenant
ssh test-isolated "grep tenant-a /var/log/gsserver/audit.log | wc -l"
ssh test-isolated "grep tenant-b /var/log/gsserver/audit.log | wc -l"
# Expected: Different logs for each tenant
```

### Scenario D: Secret Redaction
**Location:** Any VM

```bash
# 1. Trigger error that would expose secrets (e.g., bad database)
curl -X POST http://192.168.100.11:8081/api/v1/hardening \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"invalid-tenant","requestedBy":"ui-operator","profile":"strict"}'

# 2. Check response (should be redacted)
# Response should NOT contain:
#  - Database URLs (jdbc:postgresql://...)
#  - SSH key paths (/root/.ssh/id_rsa)
#  - API tokens
#  - File paths to sensitive configs

# 3. Check internal logs (should have full details)
ssh test-baseline "tail -50 /var/log/gsserver/application.log | grep -i error"
# Internal logs SHOULD contain:
#  - Full database URL
#  - Stack traces
#  - Sensitive debug info
```

---

## 4. Automation Scripts

### Setup Script: `setup-vms.sh`

```bash
#!/bin/bash
# One-time setup of all VMs

# Variables
NETWORK_NAME="gsserver-test"
NETWORK_CIDR="192.168.100.0/24"
BASE_IMAGE="/var/lib/libvirt/images/ubuntu-22.04-base.qcow2"
VMS=("dev-base" "test-baseline" "test-hardened" "test-isolated")

# Create network
virsh net-create vm-network.xml

# Create VMs from base image
for vm in "${VMS[@]}"; do
  cp "$BASE_IMAGE" "/var/lib/libvirt/images/${vm}.qcow2"
  virt-install --name "$vm" \
    --disk "/var/lib/libvirt/images/${vm}.qcow2" \
    --network "$NETWORK_NAME" \
    --memory 8192 --vcpus 4 \
    --boot hd --noautoconsole
done

echo "VMs created. Configure each with setup-vm-initial.sh"
```

### Deploy Script: `deploy-to-test.sh`

```bash
#!/bin/bash
# Deploy application to test VMs

JAR_FILE="${1:-target/gsserver-jar.jar}"
TEST_VMS=("192.168.100.11" "192.168.100.12" "192.168.100.13")

for vm_ip in "${TEST_VMS[@]}"; do
  echo "Deploying to $vm_ip..."
  
  # Copy JAR
  scp "$JAR_FILE" "deploy@$vm_ip:/opt/gsserver/gsserver.jar"
  
  # Restart service
  ssh "deploy@$vm_ip" "sudo systemctl restart gsserver"
  
  # Health check
  sleep 5
  curl -s "http://$vm_ip:8080/api/v1/health" | jq .
done
```

### Test Script: `test-hardening.sh`

```bash
#!/bin/bash
# Run end-to-end hardening test

BASELINE_IP="192.168.100.11"
HARDENED_IP="192.168.100.12"

echo "=== Baseline Metrics ==="
curl -s "http://$BASELINE_IP:8081/api/v1/metrics" | jq .

echo "=== Triggering Hardening on Test-Hardened ==="
curl -X POST "http://$HARDENED_IP:8082/api/v1/hardening" \
  -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId":"tenant-a",
    "requestedBy":"ui-operator",
    "profile":"strict"
  }' | jq .

sleep 10

echo "=== Post-Hardening Metrics ==="
curl -s "http://$HARDENED_IP:8082/api/v1/metrics" | jq .

echo "=== Security Audit (Baseline) ==="
ssh "deploy@$BASELINE_IP" "sudo iptables -L | grep -c DROP"

echo "=== Security Audit (Hardened) ==="
ssh "deploy@$HARDENED_IP" "sudo iptables -L | grep -c DROP"
```

---

## 5. Phase Gates

### Before Running on TEST-BASELINE
- [ ] Phase 1.01: Secrets Redaction complete (errorRedactor, handler, service)
- [ ] Handler focused IT passing
- [ ] Service focused IT passing
- [ ] No secrets in error responses

### Before Running on TEST-HARDENED
- [ ] Phase 1.02: Audit Logging implemented (all operations logged with actor/action/timestamp)
- [ ] Phase 1.03: Structured Errors complete (errorId, correlationId propagated through stack)
- [ ] Adapter layer integration complete
- [ ] Repository layer complete
- [ ] E2E Playwright test passing
- [ ] Cross-tenant denial tests passing

### Before Running on TEST-ISOLATED
- [ ] Multi-tenant isolation verified in code review
- [ ] Separate database per tenant configured
- [ ] Authorization checks enforced at Service layer
- [ ] Audit logs separated by tenant
- [ ] Correlation IDs don't leak tenant data

---

## 6. Success Criteria

| Scenario | Criteria | Status |
|----------|----------|--------|
| **Baseline** | Application starts, all APIs respond, no hardening applied | ⏳ Pending |
| **Hardened** | Hardening applies successfully, app functions normally, security hardened | ⏳ Pending |
| **Isolated** | Cross-tenant denial enforced, no data leakage, isolation proven | ⏳ Pending |
| **Secrets** | No secrets in API responses, internal logs contain full details | ⏳ Pending |
| **Performance** | <5% latency regression, throughput maintained post-hardening | ⏳ Pending |

---

## 7. Next Steps

1. **Complete Phase 1 security foundation** (Secrets Redaction, Audit Logging, Structured Errors)
2. **Set up hypervisor** on development machine (KVM recommended for Linux)
3. **Create base VM image** (Ubuntu 22.04 with Java/Maven/Postgres)
4. **Clone to 4 VMs** (DEV-BASE, TEST-BASELINE, TEST-HARDENED, TEST-ISOLATED)
5. **Run Scenario A** (Baseline functionality)
6. **Run Scenario B** (Hardening validation)
7. **Run Scenario C** (Cross-tenant isolation)
8. **Run Scenario D** (Secret redaction verification)

---

**Document Status:** Planning Phase  
**Last Updated:** 2026-07-24  
**Owner:** Development Team
