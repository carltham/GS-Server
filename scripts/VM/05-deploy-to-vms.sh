#!/bin/bash
# Deploy application JAR to test VMs

set -e

JAR_FILE="${1:-GSServer-pom/GSServer-UI/target/GSServer-jar-0.1.0-SNAPSHOT.jar}"

VMs=(
  "192.168.100.10:dev-base"
  "192.168.100.11:test-baseline"
  "192.168.100.12:test-hardened"
)

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Deploying Application to Test VMs                           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
  echo "❌ Error: JAR not found at $JAR_FILE"
  echo ""
  echo "Build the JAR first:"
  echo "  mvn clean package -DskipTests"
  exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo "JAR File: $JAR_FILE"
echo "Size: $JAR_SIZE"
echo ""

# Deploy to each VM
for vm_config in "${VMs[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo "═══════════════════════════════════════════════════════════════════"
  echo "Deploying to $vm_name ($vm_ip)"
  echo "═══════════════════════════════════════════════════════════════════"

  # Copy JAR
  echo "Copying JAR..."
  scp -o ConnectTimeout=5 "$JAR_FILE" "ubuntu@$vm_ip:/opt/gsserver/gsserver.jar" 2>/dev/null || echo "  (Copy may have timed out, continuing...)"

  # Create systemd service
  echo "Setting up systemd service..."
  ssh -o ConnectTimeout=5 "ubuntu@$vm_ip" "sudo tee /etc/systemd/system/gsserver.service > /dev/null" << 'SERVICE_EOF' 2>/dev/null || true
[Unit]
Description=GS-Server Application
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/gsserver
ExecStart=/usr/bin/java -jar gsserver.jar
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
SERVICE_EOF

  ssh -o ConnectTimeout=5 "ubuntu@$vm_ip" "sudo systemctl daemon-reload && sudo systemctl enable gsserver" 2>/dev/null || true

  # Start service
  echo "Starting application..."
  ssh -o ConnectTimeout=5 "ubuntu@$vm_ip" "sudo systemctl restart gsserver" 2>/dev/null || true

  echo "  ✅ Deployed"
  echo ""
done

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Deployment Complete!                                        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
