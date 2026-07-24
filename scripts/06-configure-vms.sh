#!/bin/bash
# Configure each VM (hostname, users, networking, etc.)

set -e

# VM configurations: ip:hostname
VMs=(
  "192.168.100.10:dev-base"
  "192.168.100.11:test-baseline"
  "192.168.100.12:test-hardened"
  "192.168.100.13:test-isolated"
)

CONFIGURE_SCRIPT='/tmp/configure-vm.sh'
SSH_USER="ubuntu"

echo "=== Starting all VMs ==="
for vm in dev-base test-baseline test-hardened test-isolated; do
  if ! sudo virsh list | grep -q "^ *$vm "; then
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

  # Wait for SSH to be available
  for i in {1..30}; do
    if ping -c 1 "$vm_ip" &>/dev/null && timeout 2 ssh -o ConnectTimeout=1 "$SSH_USER@$vm_ip" "echo ok" &>/dev/null; then
      echo " ✅"
      break
    fi
    echo -n "."
    sleep 2
  done
done

echo ""
echo "=== Creating configuration script ==="

cat > "$CONFIGURE_SCRIPT" << 'INNER_SCRIPT'
#!/bin/bash
set -e

VM_IP="$1"
VM_NAME="$2"

echo "Configuring $VM_NAME ($VM_IP)..."

# Update hostname
sudo hostnamectl set-hostname "$VM_NAME"

# Set timezone
sudo timedatectl set-timezone UTC

# Update system
sudo apt update
sudo apt install -y openjdk-21-jdk maven postgresql postgresql-contrib

# Create deploy user
if ! id deploy &>/dev/null; then
  sudo useradd -m -s /bin/bash deploy
  echo "deploy:deploy-password" | sudo chpasswd
  sudo usermod -aG sudo deploy
  sudo usermod -aG libvirt deploy
fi

# Enable SSH password auth
sudo sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo systemctl restart ssh

# Configure PostgreSQL
sudo systemctl start postgresql
sudo -u postgres createdb gsserver 2>/dev/null || true

# Create application directory
sudo mkdir -p /opt/gsserver
sudo chown deploy:deploy /opt/gsserver

# Create log directory
sudo mkdir -p /var/log/gsserver
sudo chown deploy:deploy /var/log/gsserver

echo "✅ Configuration complete for $VM_NAME"
INNER_SCRIPT

chmod +x "$CONFIGURE_SCRIPT"

echo ""
echo "=== Configuring VMs ==="
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo "Configuring $vm_name..."
  ssh "$SSH_USER@$vm_ip" "bash -s '$vm_ip' '$vm_name'" < "$CONFIGURE_SCRIPT"
  echo "  ✅ Done"
done

echo ""
echo "=== Setting static IPs ==="
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo "Setting static IP for $vm_name ($vm_ip)..."

  # Configure netplan for static IP
  cat > /tmp/netplan-config.yaml << NETPLAN_EOF
network:
  version: 2
  ethernets:
    eth0:
      dhcp4: no
      addresses: [$vm_ip/24]
      gateway4: 192.168.100.1
      nameservers:
        addresses: [8.8.8.8, 8.8.4.4]
NETPLAN_EOF

  # Copy and apply
  ssh "$SSH_USER@$vm_ip" "cat > /tmp/netplan.yaml" < /tmp/netplan-config.yaml
  ssh "$SSH_USER@$vm_ip" "sudo cp /tmp/netplan.yaml /etc/netplan/00-installer-config.yaml && sudo netplan apply"

  echo "  ✅ Done"
done

echo ""
echo "=== Taking snapshots ==="
for vm in dev-base test-baseline test-hardened test-isolated; do
  echo "Snapshotting $vm..."
  sudo virsh snapshot-create-as "$vm" "initial-setup" "After base setup, before app deployment" 2>/dev/null || true
done

echo ""
echo "✅ VM configuration complete!"
echo ""
echo "VMs are ready at:"
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"
  echo "  $vm_name: $vm_ip (ssh deploy@$vm_ip, password: deploy-password)"
done

echo ""
echo "Test connectivity:"
echo "  ssh deploy@192.168.100.10  # dev-base"
echo "  ssh deploy@192.168.100.11  # test-baseline"
echo ""
echo "Next: Build application JAR and deploy to test VMs"
