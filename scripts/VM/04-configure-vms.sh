#!/bin/bash
# Configure each VM (hostname, users, packages, networking)

set -e

# VM configurations: ip:hostname
VMs=(
  "192.168.100.10:dev-base"
  "192.168.100.11:test-baseline"
  "192.168.100.12:test-hardened"
  "192.168.100.13:test-isolated"
)

SSH_USER="ubuntu"

echo "=== Starting all VMs ==="
for vm in dev-base test-baseline test-hardened test-isolated; do
  if ! sudo virsh list 2>/dev/null | grep -q "^ *$vm "; then
    echo "Starting $vm..."
    sudo virsh start "$vm"
    sleep 2
  else
    echo "$vm already running"
  fi
done

sleep 5

echo ""
echo "=== Waiting for VMs to be network-ready ==="
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo -n "Waiting for $vm_name ($vm_ip)..."

  for i in {1..30}; do
    if ping -c 1 "$vm_ip" &>/dev/null 2>&1; then
      echo " ✅"
      break
    fi
    echo -n "."
    sleep 2
  done
done

echo ""
echo "=== Configuring VMs ==="
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo "Configuring $vm_name..."

  # Update hostname
  ssh -o ConnectTimeout=5 "$SSH_USER@$vm_ip" "sudo hostnamectl set-hostname '$vm_name'" 2>/dev/null || true

  # Set timezone
  ssh -o ConnectTimeout=5 "$SSH_USER@$vm_ip" "sudo timedatectl set-timezone UTC" 2>/dev/null || true

  # Update and install packages
  ssh -o ConnectTimeout=5 "$SSH_USER@$vm_ip" "sudo apt update && sudo apt install -y openjdk-21-jdk maven postgresql" 2>/dev/null || true

  # Create application directory
  ssh -o ConnectTimeout=5 "$SSH_USER@$vm_ip" "sudo mkdir -p /opt/gsserver && sudo chown ubuntu:ubuntu /opt/gsserver" 2>/dev/null || true

  echo "  ✅ Done"
done

echo ""
echo "✅ VM configuration complete!"
echo ""
echo "VMs ready at:"
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"
  echo "  $vm_name: $vm_ip"
done

echo ""
echo "Next: Run 05-deploy-to-vms.sh"
