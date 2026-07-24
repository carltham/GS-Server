#!/bin/bash
# Create VM definitions in virt-manager

set -e

VIRT_DIR="/mnt/STORAGE/VM_KVM"

# VM definitions: name:description
VMs=(
  "dev-base:Development environment for code changes"
  "test-baseline:Baseline system (control group, no hardening)"
  "test-hardened:Hardened system (after hardening applied)"
  "test-isolated:Multi-tenant isolation testing"
)

echo "=== Creating VM definitions ==="
echo ""

for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_name vm_desc <<< "$vm_config"

  # Check if VM already exists
  if sudo virsh list --all 2>/dev/null | grep -q "^ *$vm_name "; then
    echo "VM '$vm_name' already exists. Skipping..."
    continue
  fi

  echo "Creating VM: $vm_name"
  echo "  Description: $vm_desc"

  # Create VM using virt-install
  sudo virt-install \
    --name "$vm_name" \
    --memory 8192 \
    --vcpus 4 \
    --disk "$VIRT_DIR/$vm_name.qcow2" \
    --network network=gsserver-test \
    --import \
    --noautoconsole \
    --graphics none \
    --console pty,target_type=serial \
    2>&1 | grep -E "(Creating|Shutting|Starting)" || true

  echo "  ✅ Created"
  echo ""
done

echo "✅ VM definitions created!"
echo ""
echo "VMs ready:"
sudo virsh list --all

echo ""
echo "Next: Run 04-configure-vms.sh"
