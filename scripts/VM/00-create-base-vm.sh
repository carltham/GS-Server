#!/bin/bash
# Create base VM from Ubuntu ISO

set -e

ISO_FILE="${1:-/mnt/STORAGE/iso/ubuntu-24.04.4-live-server-amd64.iso}"
VIRT_DIR="/mnt/STORAGE/VM_KVM"
BASE_VM="ubuntu-24.04.4"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Creating Base VM from Ubuntu ISO                            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if ISO exists
if [ ! -f "$ISO_FILE" ]; then
  echo "❌ Error: ISO not found at $ISO_FILE"
  echo ""
  echo "Download Ubuntu ISO first:"
  echo "  wget https://releases.ubuntu.com/24.04/ubuntu-24.04.3-live-server-amd64.iso -O $ISO_FILE"
  exit 1
fi

echo "ISO File: $ISO_FILE"
echo "Storage: $VIRT_DIR/$BASE_VM.qcow2"
echo ""

# Check if VM already exists
if sudo virsh list --all 2>/dev/null | grep -q "^ *$BASE_VM "; then
  echo "❌ Error: VM '$BASE_VM' already exists"
  echo "Delete it first: sudo virsh undefine $BASE_VM --remove-all-storage"
  exit 1
fi

# Create VM
echo "Creating base VM..."
sudo virt-install \
  --name "$BASE_VM" \
  --memory 8192 \
  --vcpus 4 \
  --disk "$VIRT_DIR/$BASE_VM.qcow2,size=50" \
  --cdrom "$ISO_FILE" \
  --network network=gsserver-test \
  --graphics vnc \
  --console pty,target_type=serial \
  --noautoconsole

echo ""
echo "✅ Base VM created!"
echo ""
echo "Next steps:"
echo "  1. Open virt-manager GUI"
echo "  2. Connect to the vnc console for $BASE_VM"
echo "  3. Complete Ubuntu 24.04.4 installation:"
echo "     - Select all defaults"
echo "     - Enable OpenSSH server"
echo "  4. After install, SSH in and run:"
echo "     sudo apt update"
echo "     sudo apt install -y openjdk-21-jdk maven postgresql"
echo "  5. Shutdown: sudo shutdown -h now"
echo "  6. Then run: ./02-clone-vms.sh"
echo ""
echo "To connect via SSH (after install):"
echo "  ssh ubuntu@192.168.100.10"
