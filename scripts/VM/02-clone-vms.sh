#!/bin/bash
# Clone base VM image to create 4 test VMs

set -e

VIRT_DIR="/mnt/STORAGE/VM_KVM"
BASE_IMAGE="ubuntu-base.qcow2"

VMs=("dev-base" "test-baseline" "test-hardened" "test-isolated")

echo "=== Cloning VMs from base image ==="
echo "Source: $VIRT_DIR/$BASE_IMAGE"
echo ""

# Verify base image exists
if [ ! -f "$VIRT_DIR/$BASE_IMAGE" ]; then
  echo "❌ Error: Base image not found at $VIRT_DIR/$BASE_IMAGE"
  echo "   Please create base VM first (manually in virt-manager)"
  echo "   Storage location: $VIRT_DIR/ubuntu-base.qcow2"
  exit 1
fi

# Clone each VM
for vm in "${VMs[@]}"; do
  echo "Cloning $vm.qcow2..."
  sudo virsh vol-clone \
    --pool default \
    "$BASE_IMAGE" \
    "$vm.qcow2"
  echo "  ✅ Done"
done

echo ""
echo "✅ VM cloning complete!"
echo ""
echo "Disk images created:"
ls -lh "$VIRT_DIR"/*.qcow2 | grep -E "(dev-base|test-baseline|test-hardened|test-isolated)"

echo ""
echo "Next: Run 03-create-vms.sh"
