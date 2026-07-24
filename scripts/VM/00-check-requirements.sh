#!/bin/bash
# Pre-flight checks for VM testing environment

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Checking Requirements for VM Testing Environment            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_command() {
  local cmd="$1"
  local name="$2"

  if command -v "$cmd" &>/dev/null; then
    echo -e "${GREEN}✅${NC} $name found"
    return 0
  else
    echo -e "${RED}❌${NC} $name NOT found"
    ERRORS=$((ERRORS + 1))
    return 1
  fi
}

check_service() {
  local service="$1"
  local name="$2"

  if sudo systemctl is-active --quiet "$service" 2>/dev/null; then
    echo -e "${GREEN}✅${NC} $name is running"
    return 0
  else
    echo -e "${YELLOW}⚠️${NC}  $name is NOT running"
    WARNINGS=$((WARNINGS + 1))
    return 1
  fi
}

check_kvm() {
  if grep -q "vmx\|svm" /proc/cpuinfo; then
    echo -e "${GREEN}✅${NC} KVM virtualization supported"
    return 0
  else
    echo -e "${RED}❌${NC} KVM virtualization NOT supported"
    ERRORS=$((ERRORS + 1))
    return 1
  fi
}

check_permissions() {
  if groups "$USER" | grep -q libvirt; then
    echo -e "${GREEN}✅${NC} User has libvirt permissions"
    return 0
  else
    echo -e "${RED}❌${NC} User NOT in libvirt group"
    ERRORS=$((ERRORS + 1))
    return 1
  fi
}

check_disk_space() {
  local required_gb=100
  local available_gb=$(df /mnt/STORAGE/VM_KVM 2>/dev/null | awk 'NR==2 {print int($4/1024/1024)}' || echo 0)

  if [ "$available_gb" -ge "$required_gb" ]; then
    echo -e "${GREEN}✅${NC} Disk space: ${available_gb}GB available"
    return 0
  else
    echo -e "${RED}❌${NC} Disk space: only ${available_gb}GB available (need ${required_gb}GB)"
    ERRORS=$((ERRORS + 1))
    return 1
  fi
}

check_ram() {
  local required_gb=16
  local available_gb=$(free -g | awk 'NR==2 {print $2}')

  if [ "$available_gb" -ge "$required_gb" ]; then
    echo -e "${GREEN}✅${NC} RAM: ${available_gb}GB available"
    return 0
  else
    echo -e "${YELLOW}⚠️${NC}  RAM: only ${available_gb}GB available (recommended ${required_gb}GB)"
    WARNINGS=$((WARNINGS + 1))
    return 1
  fi
}

# Run checks
echo "=== System Checks ==="
echo ""

check_command "virsh" "virsh (libvirt CLI)"
check_command "virt-install" "virt-install"
check_command "virt-manager" "virt-manager (optional, GUI)"
echo ""

echo "=== Virtualization Support ==="
echo ""

check_kvm
check_command "qemu-system-x86_64" "QEMU emulator"
echo ""

echo "=== Service Status ==="
echo ""

check_service "libvirtd" "libvirt daemon"
echo ""

echo "=== Permissions ==="
echo ""

check_permissions
echo ""

echo "=== Resources ==="
echo ""

check_ram
check_disk_space
echo ""

# Summary
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                         Summary                                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

if [ $ERRORS -eq 0 ]; then
  echo -e "${GREEN}✅ All requirements satisfied!${NC}"
  echo ""
  echo "Ready to proceed with VM setup."
  exit 0
else
  echo -e "${RED}❌ ${ERRORS} critical requirements NOT met${NC}"
  echo ""
  echo "Fix issues and try again:"
  echo "  1. Ensure user is in libvirt group: sudo usermod -aG libvirt \$USER"
  echo "  2. Ensure libvirt daemon is running: sudo systemctl start libvirtd"
  echo "  3. Ensure /mnt/STORAGE/VM_KVM has 100GB+ space"
  echo ""
  exit 1
fi
