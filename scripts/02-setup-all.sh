#!/bin/bash
# Master setup script - orchestrates all VM setup steps

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ISO_PATH="${1:-$HOME/Downloads/ubuntu-22.04.3-live-server-amd64.iso}"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     GS-Server VM Testing Environment Setup                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check requirements first
echo "Pre-flight checks..."
echo ""
if ! bash "$SCRIPT_DIR/00-check-requirements.sh"; then
  echo ""
  echo "Failed requirements. Install missing components with:"
  echo "  ./scripts/01-install-virt-manager.sh"
  exit 1
fi
echo ""

# Check if ISO exists
if [ ! -f "$ISO_PATH" ]; then
  echo "❌ Error: Ubuntu ISO not found at $ISO_PATH"
  echo ""
  echo "Download Ubuntu 22.04 ISO first:"
  echo "  wget https://releases.ubuntu.com/22.04/ubuntu-22.04.3-live-server-amd64.iso -O $ISO_PATH"
  exit 1
fi

echo "ISO found: $ISO_PATH"
echo ""

# Step 1: Setup network
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 1: Setup Internal Network"
echo "═══════════════════════════════════════════════════════════════════"
bash "$SCRIPT_DIR/03-setup-network.sh"
echo ""

# Prompt user to create base VM
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 2: Create Base VM (MANUAL)"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "⚠️  You need to create the base VM manually in virt-manager:"
echo ""
echo "  1. Open virt-manager"
echo "  2. Click 'Create new virtual machine'"
echo "  3. Choose 'Local install media' → Select ISO: $ISO_PATH"
echo "  4. Memory: 8192 MB, CPU: 4 cores"
echo "  5. Storage: 50 GB, location: /mnt/STORAGE/VM_KVM/ubuntu-base.qcow2"
echo "  6. Name: ubuntu-base"
echo "  7. Network: gsserver-test"
echo "  8. Install Ubuntu 22.04"
echo "  9. Enable OpenSSH server during install"
echo " 10. After install, SSH in and run:"
echo "     sudo apt update && sudo apt install -y openjdk-21-jdk maven postgresql"
echo " 11. Then shutdown: sudo shutdown -h now"
echo ""
read -p "Press ENTER when base VM is created and shut down..."
echo ""

# Step 2: Clone VMs
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 2: Clone VMs from Base Image"
echo "═══════════════════════════════════════════════════════════════════"
bash "$SCRIPT_DIR/04-clone-vms.sh"
echo ""

# Step 3: Create VM definitions
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 3: Create VM Definitions"
echo "═══════════════════════════════════════════════════════════════════"
bash "$SCRIPT_DIR/05-create-vms.sh"
echo ""

read -p "Press ENTER to continue with VM configuration..."
echo ""

# Step 4: Configure VMs
echo "═══════════════════════════════════════════════════════════════════"
echo "Step 4: Configure VMs"
echo "═══════════════════════════════════════════════════════════════════"
bash "$SCRIPT_DIR/06-configure-vms.sh"
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ VM Setup Complete!                                          ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║                                                                 ║"
echo "║  VMs Ready:                                                     ║"
echo "║    dev-base:       192.168.100.10  (Development)              ║"
echo "║    test-baseline:  192.168.100.11  (Control group)            ║"
echo "║    test-hardened:  192.168.100.12  (Hardened)                 ║"
echo "║    test-isolated:  192.168.100.13  (Multi-tenant)             ║"
echo "║                                                                 ║"
echo "║  Login: ssh deploy@192.168.100.X  (password: deploy-password) ║"
echo "║                                                                 ║"
echo "║  Next Steps:                                                    ║"
echo "║    1. Build JAR: mvn clean package -DskipTests                ║"
echo "║    2. Deploy:   ./scripts/07-deploy-to-vms.sh                ║"
echo "║    3. Test:     ./scripts/08-test-hardening.sh                ║"
echo "║                                                                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
