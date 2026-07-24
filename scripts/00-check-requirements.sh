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

  if sudo systemctl is-active --quiet "$service"; then
    echo -e "${GREEN}✅${NC} $name is running"
    return 0
  else
    echo -e "${YELLOW}⚠️${NC}  $name is NOT running (will auto-start)"
    WARNINGS=$((WARNINGS + 1))
    return 1
  fi
}

check_kvm() {
  if grep -q "vmx\|svm" /proc/cpuinfo; then
    echo -e "${GREEN}✅${NC} KVM virtualization supported"
    return 0
  else
    echo -e "${RED}❌${NC} KVM virtualization NOT supported (CPU must support VT-x or AMD-V)"
    ERRORS=$((ERRORS + 1))
    return 1
  fi
}

check_permissions() {
  if groups "$USER" | grep -q libvirt; then
    echo -e "${GREEN}✅${NC} User has libvirt permissions"
    return 0
  else
    echo -e "${YELLOW}⚠️${NC}  User NOT in libvirt group"
    WARNINGS=$((WARNINGS + 1))
    return 1
  fi
}

check_disk_space() {
  local required_gb=100
  local available_gb=$(df /mnt/STORAGE/VM_KVM 2>/dev/null | awk 'NR==2 {print int($4/1024/1024)}' || df ~/ | awk 'NR==2 {print int($4/1024/1024)}')

  if [ -z "$available_gb" ]; then
    available_gb=$(df / | awk 'NR==2 {print int($4/1024/1024)}')
  fi

  if [ "$available_gb" -ge "$required_gb" ]; then
    echo -e "${GREEN}✅${NC} Disk space: ${available_gb}GB available (need ${required_gb}GB)"
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
    echo -e "${GREEN}✅${NC} RAM: ${available_gb}GB available (need ${required_gb}GB for 4 VMs)"
    return 0
  else
    echo -e "${YELLOW}⚠️${NC}  RAM: only ${available_gb}GB available (recommended ${required_gb}GB for 4 VMs)"
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

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
  echo -e "${GREEN}✅ All requirements satisfied!${NC}"
  echo ""
  echo "You can proceed with VM setup:"
  echo "  ./scripts/00-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso"
  exit 0

elif [ $ERRORS -eq 0 ]; then
  echo -e "${YELLOW}⚠️  All critical requirements met, but ${WARNINGS} warnings:${NC}"
  echo ""
  echo "You can proceed, but you may want to fix:"
  echo "  1. Add user to libvirt group:"
  echo "     sudo usermod -aG libvirt \$USER"
  echo "     # Log out and back in for changes to take effect"
  echo ""
  echo "  2. Start libvirt daemon:"
  echo "     sudo systemctl start libvirtd"
  echo "     sudo systemctl enable libvirtd"
  echo ""
  echo "Proceed with setup? (y/n)"
  read -r response
  if [ "$response" = "y" ]; then
    exit 0
  else
    exit 1
  fi

else
  echo -e "${RED}❌ ${ERRORS} critical requirements NOT met${NC}"
  echo ""
  echo "You need to install/fix the missing components:"
  echo ""
  echo "Option 1: Run installation script (recommended)"
  echo "  ./scripts/00-install-virt-manager.sh"
  echo ""
  echo "Option 2: Manual installation"
  echo "  https://ubuntu.com/server/docs/virtualization-qemu"
  echo ""
  exit 1
fi
