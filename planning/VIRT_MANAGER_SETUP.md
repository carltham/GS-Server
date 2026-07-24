# VM Setup Guide: virt-manager + QEMU/KVM

Quick steps to create the 4-VM testing environment.

---

## Step 1: Create Internal Network

```bash
# Create network config file
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

# Create network (requires sudo)
sudo virsh net-define /tmp/gsserver-network.xml
sudo virsh net-start gsserver-test
sudo virsh net-autostart gsserver-test

# Verify
sudo virsh net-list
```

**Expected output:**
```
Name             State      Autostart   Persistent
gsserver-test    active     yes         yes
```

---

## Step 2: Download Ubuntu 22.04 ISO

```bash
# Download (4.5 GB)
cd ~/Downloads
wget https://releases.ubuntu.com/22.04/ubuntu-22.04.3-live-server-amd64.iso

# Verify checksum
sha256sum ubuntu-22.04.3-live-server-amd64.iso
# Should match: https://releases.ubuntu.com/22.04/SHA256SUMS
```

---

## Step 3: Create Base VM (Template)

**In virt-manager GUI:**

1. Click "Create new virtual machine"
2. **Step 1: Choose Installation Media**
   - Select "Local install media"
   - Browse to `~/Downloads/ubuntu-22.04.3-live-server-amd64.iso`

3. **Step 2: Unattended Installation (Optional)**
   - Skip (we'll install manually for control)

4. **Step 3: Memory and CPU**
   - Memory: `8192 MB`
   - CPU: `4` cores

5. **Step 4: Storage**
   - Create disk image on computer
   - Size: `50 GB`
   - Storage format: `qcow2` (copy-on-write)
   - Location: `/var/lib/libvirt/images/ubuntu-base.qcow2`

6. **Step 5: Ready to Start**
   - Name: `ubuntu-base`
   - Network: `gsserver-test`
   - **Finish**

7. **Ubuntu Installation (in VM console):**
   ```
   - Language: English
   - Keyboard: Your preference
   - Network: DHCP (auto)
   - Proxy: Skip
   - Storage: Use entire disk (default)
   - Username: ubuntu
   - Password: your-secure-password
   - OpenSSH server: YES (required)
   - Snaps: Skip
   ```

8. **After installation, in ubuntu-base VM:**
   ```bash
   sudo apt update
   sudo apt install -y openjdk-21-jdk maven postgresql postgresql-contrib
   
   # Enable SSH password auth (for easier copying)
   sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config
   sudo systemctl restart ssh
   
   # Shutdown
   sudo shutdown -h now
   ```

9. **In virt-manager, verify ubuntu-base is shut down**

---

## Step 4: Clone Base Image to 4 VMs

```bash
# Define the 4 VMs we need
VIRT_DIR="/var/lib/libvirt/images"

# Arrays for VM names and IPs
VMs=("dev-base" "test-baseline" "test-hardened" "test-isolated")

# Clone base image for each VM
for vm in "${VMs[@]}"; do
  echo "Cloning $vm from ubuntu-base..."
  
  # Use virsh to clone (creates COW clone - instant, minimal storage)
  sudo virsh vol-clone \
    --pool default \
    ubuntu-base.qcow2 \
    $vm.qcow2
done

echo "Cloning complete. Disks created:"
sudo ls -lh $VIRT_DIR/*.qcow2
```

**Expected output (cloning is instant with COW):**
```
Cloning dev-base from ubuntu-base...
Vol dev-base.qcow2 cloned from ubuntu-base.qcow2
Cloning test-baseline from ubuntu-base...
Vol test-baseline.qcow2 cloned from test-baseline.qcow2
Cloning test-hardened from ubuntu-base...
Vol test-hardened.qcow2 cloned from test-hardened.qcow2
Cloning test-isolated from ubuntu-base...
Vol test-isolated.qcow2 cloned from test-isolated.qcow2

Disks created:
-rw-r--r-- 2.3G ubuntu-base.qcow2
-rw-r--r-- 196K dev-base.qcow2
-rw-r--r-- 196K test-baseline.qcow2
-rw-r--r-- 196K test-hardened.qcow2
-rw-r--r-- 196K test-isolated.qcow2
```

---

## Step 5: Create VM Definitions in virt-manager

**For each VM (repeat 4 times with different names):**

```bash
# Script to create VM definitions
cat > /tmp/create-vms.sh << 'SCRIPT'
#!/bin/bash

VMS=(
  "dev-base:192.168.100.10"
  "test-baseline:192.168.100.11"
  "test-hardened:192.168.100.12"
  "test-isolated:192.168.100.13"
)

VIRT_DIR="/var/lib/libvirt/images"

for vm_config in "${VMS[@]}"; do
  IFS=':' read -r vm_name vm_ip <<< "$vm_config"
  
  echo "Creating VM: $vm_name (IP: $vm_ip)"
  
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
    --console pty,target_type=serial
done

SCRIPT

chmod +x /tmp/create-vms.sh
/tmp/create-vms.sh
```

---

## Step 6: Configure Each VM

**Start each VM and configure:**

```bash
# Start all VMs
for vm in dev-base test-baseline test-hardened test-isolated; do
  sudo virsh start $vm
done

# Check they're running
sudo virsh list --all
```

**For each VM, SSH in and configure:**

```bash
# Get IP address
sudo virsh domifaddr dev-base

# SSH into dev-base
ssh ubuntu@192.168.100.10
# Password: (whatever you set during install)

# Inside VM:
sudo hostnamectl set-hostname dev-base
sudo timedatectl set-timezone UTC

# Create deploy user
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG sudo deploy
sudo passwd deploy

# Configure PostgreSQL (dev-base only)
sudo systemctl start postgresql
sudo -u postgres createdb gsserver

# Create test database (on each VM)
sudo -u postgres psql << EOF
CREATE USER gsserver WITH PASSWORD 'test-password';
ALTER USER gsserver WITH SUPERUSER;
EOF

# Enable SSH for deploy user
sudo mkdir -p /home/deploy/.ssh
sudo cp ~/.ssh/authorized_keys /home/deploy/.ssh/ 2>/dev/null || \
  echo "ssh-rsa AAAA..." | sudo tee /home/deploy/.ssh/authorized_keys

sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys

# Create application directory
sudo mkdir -p /opt/gsserver
sudo chown deploy:deploy /opt/gsserver

# Logout
logout
```

**Repeat for:** test-baseline, test-hardened, test-isolated (same steps)

---

## Step 7: Set Static IPs

```bash
# On each VM, configure static IP
# SSH into each VM as deploy user

# Edit netplan config
sudo nano /etc/netplan/00-installer-config.yaml
```

**Replace with:**
```yaml
# For dev-base (192.168.100.10):
network:
  version: 2
  ethernets:
    eth0:
      dhcp4: no
      addresses: [192.168.100.10/24]
      gateway4: 192.168.100.1
      nameservers:
        addresses: [8.8.8.8, 8.8.4.4]
```

**Apply:**
```bash
sudo netplan apply
sudo systemctl restart networking

# Verify
ip addr show
```

**Repeat for test-baseline (192.168.100.11), test-hardened (192.168.100.12), test-isolated (192.168.100.13)**

---

## Step 8: Verify All VMs Are Connected

```bash
# From host machine
for ip in 192.168.100.10 192.168.100.11 192.168.100.12 192.168.100.13; do
  echo "Testing $ip..."
  ping -c 1 $ip
  ssh deploy@$ip "hostname"
done
```

**Expected output:**
```
Testing 192.168.100.10...
PING 192.168.100.10 (192.168.100.10) 56(84) bytes of data.
64 bytes from 192.168.100.10: icmp_seq=1 ttl=64 time=0.500 ms
dev-base
Testing 192.168.100.11...
...
test-baseline
...
```

---

## Step 9: Take Snapshots

```bash
# Before deploying application, take snapshots
for vm in dev-base test-baseline test-hardened test-isolated; do
  sudo virsh snapshot-create-as $vm "initial-setup" "After base setup, before app deployment"
done

# List snapshots
sudo virsh snapshot-list dev-base
```

---

## Step 10: Quick Test

```bash
# SSH into test-baseline
ssh deploy@192.168.100.11

# Verify Java and Maven
java -version
mvn -version

# Verify PostgreSQL
sudo systemctl status postgresql

# Verify network access between VMs
ping 192.168.100.10  # Should reach dev-base

# Logout
logout
```

---

## Troubleshooting

### VM won't start
```bash
sudo virsh start dev-base
sudo virsh console dev-base  # See boot logs
```

### No network connectivity
```bash
# Check network
sudo virsh net-list
sudo virsh net-info gsserver-test

# Restart network
sudo virsh net-destroy gsserver-test
sudo virsh net-start gsserver-test
```

### Can't SSH
```bash
# Check if SSH is running in VM
sudo virsh console test-baseline
# In console: sudo systemctl status ssh

# Check if IP is assigned
sudo virsh domifaddr test-baseline
```

### Need to power down all VMs
```bash
for vm in dev-base test-baseline test-hardened test-isolated; do
  sudo virsh shutdown $vm
done

# Or force shutdown
for vm in dev-base test-baseline test-hardened test-isolated; do
  sudo virsh destroy $vm
done
```

---

## Next Steps

1. ✅ Create internal network (gsserver-test)
2. ✅ Download Ubuntu ISO
3. ✅ Create base VM (ubuntu-base)
4. ✅ Clone to 4 VMs
5. ✅ Configure each VM (hostname, users, networking)
6. ✅ Take snapshots
7. **Next:** Deploy application JAR to test-baseline and test-hardened

---

**Status:** Setup Guide Ready  
**Time to Complete:** ~2 hours (mostly waiting for Ubuntu install)  
**Skills Required:** Basic Linux/SSH, virt-manager GUI basics
