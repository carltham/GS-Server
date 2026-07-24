# GS-Server VM Testing Scripts

Automated setup and testing scripts for the VM-based development and testing environment.

---

## Quick Start

**VM Storage Location:** `/mnt/STORAGE/VM_KVM` (252GB available)

```bash
# 0. Check if virt-manager is installed
./scripts/00-check-requirements.sh

# If missing, install:
# ./scripts/01-install-virt-manager.sh

# 1. Download Ubuntu ISO
wget https://releases.ubuntu.com/22.04/ubuntu-22.04.3-live-server-amd64.iso \
  -O ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso

# 2. Run master setup script (includes pre-flight checks)
./scripts/02-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso

# 3. Build application
mvn clean package -DskipTests

# 4. Deploy to VMs
./scripts/07-deploy-to-vms.sh

# 5. Test hardening
./scripts/08-test-hardening.sh

# 6. Manage VMs
./scripts/99-vm-management.sh status
```

---

## Script Reference

### Setup Scripts

#### `00-check-requirements.sh`
Verifies all prerequisites before VM setup.

```bash
./scripts/00-check-requirements.sh
```

**Checks:**
- ✅ virsh and virt-install installed
- ✅ KVM virtualization supported
- ✅ libvirtd daemon running
- ✅ User has libvirt permissions
- ✅ Sufficient disk space (100+ GB)
- ✅ Sufficient RAM (16+ GB recommended)

**Output:**
- Shows all checks with status (✅ pass, ⚠️ warning, ❌ fail)
- Suggests fixes if issues found
- Exits 0 if ready, 1 if critical issues

**When to run:**
- Before first VM setup
- To troubleshoot virt-manager issues
- To verify system meets requirements

---

#### `01-install-virt-manager.sh`
Installs virt-manager, libvirt, and QEMU/KVM.

```bash
./scripts/01-install-virt-manager.sh
```

**What it installs:**
- `qemu-kvm` - KVM hypervisor
- `qemu-system-x86` - QEMU system emulator
- `libvirt-daemon-system` - libvirt daemon
- `libvirt-clients` - libvirt CLI tools
- `virt-manager` - GUI VM management
- `virt-install` - VM creation tool
- `bridge-utils` - Network bridging

**What it configures:**
1. Updates package list
2. Installs all components
3. Enables libvirtd daemon
4. Adds user to libvirt group
5. Verifies installation

**Requires:** sudo access, Ubuntu/Debian system

**Time:** ~5 minutes

**Important:** After running, you must log out and back in for group permissions to take effect.

```bash
# After installation
logout  # or close terminal
# Log back in
groups $USER  # Should show: ... libvirt kvm
```

---

#### `02-setup-all.sh` (Master Setup)
Orchestrates the complete VM environment setup (includes pre-flight checks).

```bash
./scripts/02-setup-all.sh [path-to-iso]

# Example:
./scripts/02-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso
```

**What it does:**
1. ✅ Runs pre-flight checks (calls 00-check-requirements.sh)
2. Creates internal network (192.168.100.0/24)
3. Prompts for base VM creation (manual via virt-manager)
   - **Storage location:** `/mnt/STORAGE/VM_KVM/ubuntu-base.qcow2`
4. Clones base image to 4 VMs
5. Creates VM definitions
6. Configures each VM (hostname, users, packages)
7. Sets static IPs
8. Takes initial snapshots

**Prerequisites:**
- virt-manager, libvirt, QEMU/KVM installed
- User in libvirt group
- 100+ GB disk space at `/mnt/STORAGE/VM_KVM`
- 16+ GB RAM recommended

**VM Storage:**
- Path: `/mnt/STORAGE/VM_KVM`
- Available: 252 GB (on 897 GB /dev/sda1)
- VM disk images stored as `.qcow2` (copy-on-write format)

**If prerequisites fail:**
Script will suggest running:
```bash
./scripts/01-install-virt-manager.sh
```

**Time:** ~2 hours (mostly Ubuntu installation)

---

#### `03-setup-network.sh`
Creates the isolated internal network for VMs.

```bash
./scripts/03-setup-network.sh
```

**Network Details:**
- Network: `gsserver-test`
- CIDR: `192.168.100.0/24`
- Gateway: `192.168.100.1`
- DHCP range: `192.168.100.10-254`

---

#### `04-clone-vms.sh`
Clones the base VM image to create 4 test VMs (instant with COW).

```bash
./scripts/04-clone-vms.sh
```

**VMs Created:**
- `dev-base.qcow2` (196 KB - COW clone)
- `test-baseline.qcow2` (196 KB - COW clone)
- `test-hardened.qcow2` (196 KB - COW clone)
- `test-isolated.qcow2` (196 KB - COW clone)

---

#### `05-create-vms.sh`
Creates VM definitions in virt-manager from cloned images.

```bash
./scripts/05-create-vms.sh
```

**VMs Created:**
- dev-base (192.168.100.10)
- test-baseline (192.168.100.11)
- test-hardened (192.168.100.12)
- test-isolated (192.168.100.13)

---

#### `06-configure-vms.sh`
Configures each VM (hostnames, users, packages, networking).

```bash
./scripts/06-configure-vms.sh
```

**Configuration:**
- Hostname setup
- Deploy user creation (password: deploy-password)
- OpenJDK 21, Maven, PostgreSQL installation
- Static IP configuration
- Initial snapshots

**Time:** ~5 minutes

---

### Deployment & Testing Scripts

#### `07-deploy-to-vms.sh`
Deploys application JAR to test VMs.

```bash
./scripts/07-deploy-to-vms.sh [path-to-jar]

# Examples:
./scripts/07-deploy-to-vms.sh  # Uses default path
./scripts/07-deploy-to-vms.sh GSServer-pom/GSServer-UI/target/gsserver.jar
```

**What it does:**
1. Verifies JAR exists
2. Copies JAR to `/opt/gsserver/` on each VM
3. Creates systemd service
4. Starts application
5. Verifies health check

**Deployed VMs:**
- dev-base (192.168.100.10:8080)
- test-baseline (192.168.100.11:8080)
- test-hardened (192.168.100.12:8080)

**Time:** ~2 minutes

---

#### `08-test-hardening.sh`
Runs end-to-end hardening tests across VMs.

```bash
./scripts/08-test-hardening.sh
```

**Tests:**
1. Baseline health check
2. Pre-hardening health check (test-hardened)
3. Trigger hardening operation (test-hardened)
4. Post-hardening health check
5. Get operation state
6. Performance comparison
7. Security audit (open ports, firewall rules)

**Time:** ~15 minutes

---

### Management Script

#### `99-vm-management.sh`
Utility for VM lifecycle management (start, stop, reset, etc.).

```bash
./scripts/99-vm-management.sh <command> [options]
```

**Commands:**

| Command | Description |
|---------|-------------|
| `help` | Show this help |
| `start` | Start all VMs |
| `stop` | Stop all VMs gracefully |
| `kill` | Force stop all VMs |
| `restart` | Restart all VMs |
| `status` | Show VM status |
| `list` | List VMs with IPs |
| `start-vm <name>` | Start specific VM |
| `stop-vm <name>` | Stop specific VM |
| `ssh <name>` | SSH to VM |
| `console <name>` | Open VM console |
| `snapshot <name>` | Take snapshot |
| `restore <name> <snap>` | Restore snapshot |
| `logs <name>` | Show app logs |
| `audit-log <name>` | Show audit logs |
| `cleanup` | Delete all VMs |

**Examples:**

```bash
# Start all VMs
./scripts/99-vm-management.sh start

# SSH to test-baseline
./scripts/99-vm-management.sh ssh test-baseline

# Show logs from test-hardened
./scripts/99-vm-management.sh logs test-hardened

# Take snapshot before testing
./scripts/99-vm-management.sh snapshot test-hardened

# Check VM status
./scripts/99-vm-management.sh status

# List all VMs
./scripts/99-vm-management.sh list
```

---

## VM Reference

### dev-base (192.168.100.10)
**Purpose:** Development environment

- Java 21, Maven, PostgreSQL
- Application running on port 8080
- Used for building and testing application code

**SSH Access:**
```bash
ssh deploy@192.168.100.10  # password: deploy-password
```

---

### test-baseline (192.168.100.11)
**Purpose:** Control group (baseline system, no hardening)

- Stock Ubuntu 22.04 configuration
- Application running on port 8080
- Snapshots taken before/after baseline tests

**SSH Access:**
```bash
ssh deploy@192.168.100.11  # password: deploy-password
```

---

### test-hardened (192.168.100.12)
**Purpose:** Hardened system (after hardening applied)

- Ubuntu 22.04 with hardening applied
- Application running on port 8080
- Cloned from test-baseline before applying hardening

**SSH Access:**
```bash
ssh deploy@192.168.100.12  # password: deploy-password
```

---

### test-isolated (192.168.100.13)
**Purpose:** Multi-tenant isolation testing

- Dual application instances (tenant-a, tenant-b)
- Separate databases per tenant
- Network firewall rules prevent cross-tenant access

**SSH Access:**
```bash
ssh deploy@192.168.100.13  # password: deploy-password
```

---

## Common Workflows

### Workflow 1: Initial Setup

```bash
# 1. Download ISO
wget https://releases.ubuntu.com/22.04/ubuntu-22.04.3-live-server-amd64.iso \
  -O ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso

# 2. Run master setup (interactive)
./scripts/02-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso

# Follow prompts to create base VM manually, then script completes setup
```

**Time:** ~2 hours

---

### Workflow 2: Deploy & Test

```bash
# 1. Build application
mvn clean package -DskipTests

# 2. Deploy to VMs
./scripts/07-deploy-to-vms.sh

# 3. Run tests
./scripts/08-test-hardening.sh

# 4. Inspect results
./scripts/99-vm-management.sh logs test-hardened
./scripts/99-vm-management.sh audit-log test-hardened
```

**Time:** ~5 minutes

---

### Workflow 3: Debug & Fix

```bash
# 1. SSH to problem VM
./scripts/99-vm-management.sh ssh test-hardened

# 2. Check application logs
sudo journalctl -u gsserver -f

# 3. Check audit logs
tail -f /var/log/gsserver/audit.log

# 4. Make fixes, redeploy
./scripts/07-deploy-to-vms.sh

# 5. Take snapshot after fixes
./scripts/99-vm-management.sh snapshot test-hardened
```

---

### Workflow 4: Restore from Snapshot

```bash
# 1. List available snapshots
./scripts/99-vm-management.sh console test-hardened  # Check history

# 2. Restore snapshot
./scripts/99-vm-management.sh restore test-hardened initial-setup

# 3. Redeploy application
./scripts/07-deploy-to-vms.sh
```

---

## Troubleshooting

### VMs won't start
```bash
# Check if network exists
sudo virsh net-list

# Check VM status
./scripts/99-vm-management.sh status

# Try restarting network
sudo virsh net-destroy gsserver-test
sudo virsh net-start gsserver-test

# Start VM with verbose output
sudo virsh start test-baseline
sudo virsh console test-baseline  # See boot logs
```

### Can't SSH to VM
```bash
# Check if VM is running
./scripts/99-vm-management.sh status

# Check if VM has IP
sudo virsh domifaddr test-baseline

# Try pinging VM
ping 192.168.100.11

# Check SSH service in VM
sudo virsh console test-baseline
# In console: sudo systemctl status ssh
```

### Application won't start
```bash
# SSH to VM
./scripts/99-vm-management.sh ssh test-baseline

# Check service status
sudo systemctl status gsserver

# Check logs
sudo journalctl -u gsserver -n 100

# Verify JAR exists
ls -la /opt/gsserver/gsserver.jar

# Verify Java
java -version
```

### Can't reach application API
```bash
# Test from host
curl http://192.168.100.11:8080/api/v1/health

# Test from VM
./scripts/99-vm-management.sh ssh test-baseline
curl http://localhost:8080/api/v1/health

# Check if port is listening
sudo ss -tuln | grep 8080
```

### Need to clean up and start over
```bash
# Delete all VMs (WARNING: destructive)
./scripts/99-vm-management.sh cleanup

# Then run full setup again
./scripts/02-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso
```

---

## Phase Gates

### Before Running Tests

**Phase 1.01 - Secrets Redaction:**
- [ ] Error redaction implemented
- [ ] Handler layer complete
- [ ] Service layer complete
- [ ] No secrets in error responses

**Phase 1.02 - Audit Logging:**
- [ ] Audit logging implemented
- [ ] All operations logged with actor/action/timestamp

**Phase 1.03 - Structured Errors:**
- [ ] errorId, correlationId propagated through stack
- [ ] All API responses structured

**Before system integration:**
- [ ] Repository layer complete
- [ ] E2E Playwright tests passing
- [ ] Cross-tenant denial tests passing

---

## Next Steps

1. ✅ Create setup scripts
2. ⏳ Run `02-setup-all.sh` to create VM environment
3. ⏳ Complete Phase 1 security foundation (secrets, audit, structured errors)
4. ⏳ Deploy and test with `07-deploy-to-vms.sh`
5. ⏳ Run end-to-end tests with `08-test-hardening.sh`

---

**Last Updated:** 2026-07-24  
**Author:** Development Team  
**Status:** Ready for use
