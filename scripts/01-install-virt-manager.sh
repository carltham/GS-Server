#!/bin/bash
# Install virt-manager, libvirt, and QEMU/KVM

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Installing virt-manager and KVM Virtualization              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if running on Ubuntu/Debian
if ! command -v apt &>/dev/null; then
  echo "❌ Error: This script requires Ubuntu/Debian"
  echo ""
  echo "For other distributions, see:"
  echo "  https://ubuntu.com/server/docs/virtualization-qemu"
  exit 1
fi

# Check if running as non-root (will need sudo)
if [ "$EUID" -eq 0 ]; then
  echo "⚠️  Running as root. Some steps may not work correctly."
  echo "Please run as regular user with sudo access."
  exit 1
fi

# Verify sudo access
if ! sudo -n true 2>/dev/null; then
  echo "Testing sudo access..."
  sudo true || {
    echo "❌ Error: sudo access required"
    exit 1
  }
fi

echo "✅ Sudo access verified"
echo ""

# Step 1: Update package list
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 1: Update Package List"
echo "═══════════════════════════════════════════════════════════════════"
echo "Running: sudo apt update"
sudo apt update
echo "✅ Package list updated"
echo ""

# Step 2: Install KVM and QEMU
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 2: Install KVM and QEMU"
echo "═══════════════════════════════════════════════════════════════════"
echo "Installing: qemu-kvm qemu-system-x86 cpu-checker"
sudo apt install -y qemu-kvm qemu-system-x86 cpu-checker
echo "✅ KVM and QEMU installed"
echo ""

# Step 3: Install libvirt
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 3: Install libvirt"
echo "═══════════════════════════════════════════════════════════════════"
echo "Installing: libvirt-daemon-system libvirt-clients bridge-utils"
sudo apt install -y libvirt-daemon-system libvirt-clients bridge-utils
echo "✅ libvirt installed"
echo ""

# Step 4: Install virt-manager and tools
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 4: Install virt-manager and Tools"
echo "═══════════════════════════════════════════════════════════════════"
echo "Installing: virt-manager virt-viewer virt-install"
sudo apt install -y virt-manager virt-viewer virt-install
echo "✅ virt-manager and tools installed"
echo ""

# Step 5: Enable and start libvirt daemon
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 5: Enable and Start libvirt Daemon"
echo "═══════════════════════════════════════════════════════════════════"
echo "Enabling libvirt daemon..."
sudo systemctl enable libvirtd
echo "Starting libvirt daemon..."
sudo systemctl restart libvirtd
echo "✅ libvirt daemon is running"
echo ""

# Step 6: Add user to libvirt group
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 6: Configure User Permissions"
echo "═══════════════════════════════════════════════════════════════════"

if groups "$USER" | grep -q libvirt; then
  echo "✅ User already in libvirt group"
else
  echo "Adding user to libvirt group..."
  sudo usermod -aG libvirt "$USER"

  # Also add to kvm group if it exists
  if getent group kvm &>/dev/null; then
    sudo usermod -aG kvm "$USER"
    echo "✅ User added to libvirt and kvm groups"
  else
    echo "✅ User added to libvirt group"
  fi

  echo ""
  echo "⚠️  GROUP MEMBERSHIP CHANGE DETECTED"
  echo "You need to log out and back in for the changes to take effect."
  echo ""
  echo "After logging back in, verify with:"
  echo "  groups \$USER"
  echo ""
  echo "Then run the setup script:"
  echo "  ./scripts/00-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso"
fi
echo ""

# Step 7: Verify installation
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 7: Verify Installation"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

echo -n "Checking virsh... "
if virsh --version &>/dev/null; then
  echo "✅ $(virsh --version)"
else
  echo "❌ Failed"
fi

echo -n "Checking virt-install... "
if virt-install --version &>/dev/null; then
  echo "✅ $(virt-install --version)"
else
  echo "❌ Failed"
fi

echo -n "Checking virt-manager... "
if virt-manager --version &>/dev/null; then
  echo "✅ $(virt-manager --version)"
else
  echo "⚠️  Not installed (GUI optional, can install manually)"
fi

echo -n "Checking libvirtd daemon... "
if sudo systemctl is-active --quiet libvirtd; then
  echo "✅ Running"
else
  echo "❌ Not running"
fi

echo -n "Checking KVM support... "
if grep -q "vmx\|svm" /proc/cpuinfo; then
  echo "✅ Supported"
else
  echo "❌ Not supported (CPU must support VT-x or AMD-V)"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Installation Complete!                                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

if ! groups "$USER" | grep -q libvirt; then
  echo "⚠️  GROUP MEMBERSHIP CHANGE REQUIRED"
  echo ""
  echo "Log out and back in for changes to take effect:"
  echo "  logout"
  echo "  # Log back in"
  echo "  groups \$USER  # Verify libvirt is listed"
  echo ""
  echo "Then proceed with setup:"
  echo "  ./scripts/00-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso"
else
  echo "You're ready to set up VMs!"
  echo ""
  echo "Next steps:"
  echo "  1. Download Ubuntu ISO:"
  echo "     wget https://releases.ubuntu.com/22.04/ubuntu-22.04.3-live-server-amd64.iso \\"
  echo "       -O ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso"
  echo ""
  echo "  2. Run VM setup:"
  echo "     ./scripts/00-setup-all.sh ~/Downloads/ubuntu-22.04.3-live-server-amd64.iso"
  echo ""
  echo "  3. Or verify installation with:"
  echo "     ./scripts/00-check-requirements.sh"
fi
