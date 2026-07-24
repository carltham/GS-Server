# GS-Server VM Setup Scripts

Individual scripts for VM testing environment setup.

**VM Storage:** `/mnt/STORAGE/VM_KVM` (252GB available)

---

## Quick Start

Run scripts **in order:**

```bash
cd /mnt/DATA/Projects/0.present-projects/Active/GS-Server/scripts/VM

# 1. Check requirements
./00-check-requirements.sh

# 2. Create internal network
./01-setup-network.sh

# 3. Create base VM manually in virt-manager
#    Storage: /mnt/STORAGE/VM_KVM/ubuntu-base.qcow2
#    Then run:

# 4. Clone base image to 4 VMs
./02-clone-vms.sh

# 5. Create VM definitions
./03-create-vms.sh

# 6. Configure VMs (hostnames, packages, etc)
./04-configure-vms.sh

# 7. Deploy application
./05-deploy-to-vms.sh

# 8. Test hardening
./06-test-hardening.sh

# Manage VMs
./99-vm-management.sh status
```

---

## Scripts Reference

### 00-check-requirements.sh
Verify system meets requirements.

```bash
./00-check-requirements.sh
```

**Checks:**
- virsh, virt-install, virt-manager installed
- KVM virtualization support
- libvirt daemon running
- User in libvirt group
- 100GB+ disk space
- 16GB+ RAM

---

### 01-setup-network.sh
Create internal network (192.168.100.0/24).

```bash
./01-setup-network.sh
```

---

### 02-clone-vms.sh
Clone base VM image to 4 test VMs.

```bash
./02-clone-vms.sh
```

**Requires:** ubuntu-base.qcow2 in /mnt/STORAGE/VM_KVM/

---

### 03-create-vms.sh
Create VM definitions in virt-manager.

```bash
./03-create-vms.sh
```

**VMs Created:**
- dev-base (192.168.100.10)
- test-baseline (192.168.100.11)
- test-hardened (192.168.100.12)
- test-isolated (192.168.100.13)

---

### 04-configure-vms.sh
Configure VMs (hostnames, packages, etc).

```bash
./04-configure-vms.sh
```

---

### 05-deploy-to-vms.sh
Deploy application JAR to VMs.

```bash
./05-deploy-to-vms.sh [path-to-jar]

# Default path:
./05-deploy-to-vms.sh
```

---

### 06-test-hardening.sh
Run end-to-end hardening tests.

```bash
./06-test-hardening.sh
```

---

### 99-vm-management.sh
Manage VM lifecycle.

```bash
./99-vm-management.sh <command>

# Commands:
start              # Start all VMs
stop               # Stop all VMs
kill               # Force stop all VMs
restart            # Restart all VMs
status             # Show VM status
list               # List all VMs
start-vm <name>    # Start specific VM
stop-vm <name>     # Stop specific VM
ssh <name>         # SSH to VM
logs <name>        # Show app logs
cleanup            # Delete all VMs
```

---

## Create Base VM Manually

**Before running 02-clone-vms.sh:**

1. Open virt-manager
2. Create new VM
3. Select ISO: `/home/user/Downloads/ubuntu-22.04.3-live-server-amd64.iso`
4. Memory: 8192 MB, CPU: 4 cores
5. Storage: 50 GB at `/mnt/STORAGE/VM_KVM/ubuntu-base.qcow2`
6. Network: `gsserver-test`
7. Install Ubuntu 22.04
8. Enable OpenSSH during install
9. After install, SSH in and run:
   ```bash
   sudo apt update
   sudo apt install -y openjdk-21-jdk maven postgresql
   sudo shutdown -h now
   ```

Then run `02-clone-vms.sh`

---

## Troubleshooting

### User not in libvirt group
```bash
sudo usermod -aG libvirt $USER
# Log out and back in
```

### Libvirt daemon not running
```bash
sudo systemctl start libvirtd
sudo systemctl enable libvirtd
```

### Network not found
```bash
./01-setup-network.sh
```

### VMs won't start
```bash
sudo virsh list --all
sudo virsh start <vm-name>
sudo virsh console <vm-name>  # See boot logs
```

---

**Status:** Ready for use
