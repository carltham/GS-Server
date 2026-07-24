#!/bin/bash
# Setup internal network for VM testing environment

set -e

echo "=== Creating internal network for GS-Server VMs ==="

# Create network config
cat > /tmp/gsserver-network.xml << 'EOF'
<network>
  <name>gsserver-test</name>
  <forward mode='nat'/>
  <bridge name='virbr1' stp='on' delay='0'/>
  <ip address='192.168.100.1' netmask='255.255.255.0'>
    <dhcp>
      <range start='192.168.100.10' end='192.168.100.254'/>
    </dhcp>
  </ip>
</network>
EOF

echo "Network config created at /tmp/gsserver-network.xml"

# Check if network already exists
if sudo virsh net-list | grep -q gsserver-test; then
  echo "Network 'gsserver-test' already exists. Skipping..."
  sudo virsh net-list
  exit 0
fi

# Create and start network
echo "Creating network..."
sudo virsh net-define /tmp/gsserver-network.xml
sudo virsh net-start gsserver-test
sudo virsh net-autostart gsserver-test

echo ""
echo "✅ Network setup complete!"
echo ""
echo "Network details:"
sudo virsh net-list
sudo virsh net-info gsserver-test

echo ""
echo "Next: Download Ubuntu ISO and run 02-create-base-vm.sh"
