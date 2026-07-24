#!/bin/bash
# Deploy application JAR to test VMs

set -e

JAR_FILE="${1:-GSServer-pom/GSServer-UI/target/GSServer-jar-0.1.0-SNAPSHOT.jar}"

# VM IPs
VMS=(
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
for vm_config in "${VMS[@]}"; do
  IFS=':' read -r vm_ip vm_name <<< "$vm_config"

  echo "═══════════════════════════════════════════════════════════════════"
  echo "Deploying to $vm_name ($vm_ip)"
  echo "═══════════════════════════════════════════════════════════════════"

  # Copy JAR
  echo "Copying JAR..."
  scp -q "$JAR_FILE" "deploy@$vm_ip:/opt/gsserver/gsserver.jar"
  echo "✅ JAR copied"

  # Create systemd service if not exists
  echo "Setting up systemd service..."
  ssh "deploy@$vm_ip" "sudo tee /etc/systemd/system/gsserver.service > /dev/null" << 'SERVICE_EOF'
[Unit]
Description=GS-Server Application
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/gsserver
ExecStart=/usr/bin/java -jar gsserver.jar
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
SERVICE_EOF

  ssh "deploy@$vm_ip" "sudo systemctl daemon-reload"
  ssh "deploy@$vm_ip" "sudo systemctl enable gsserver"

  # Start service
  echo "Starting application..."
  ssh "deploy@$vm_ip" "sudo systemctl restart gsserver"

  # Wait for startup
  echo -n "Waiting for application to start..."
  for i in {1..30}; do
    if curl -s "http://$vm_ip:8080/api/v1/health" >/dev/null 2>&1; then
      echo " ✅"
      break
    fi
    echo -n "."
    sleep 1
  done

  # Health check
  echo "Health check:"
  curl -s "http://$vm_ip:8080/api/v1/health" | jq . 2>/dev/null || echo "  (Application starting...)"

  echo ""
done

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Deployment Complete!                                        ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║                                                                 ║"
echo "║  Applications running at:                                       ║"
echo "║    dev-base:       http://192.168.100.10:8080                 ║"
echo "║    test-baseline:  http://192.168.100.11:8080                 ║"
echo "║    test-hardened:  http://192.168.100.12:8080                 ║"
echo "║                                                                 ║"
echo "║  Check logs: ssh deploy@192.168.100.X                          ║"
echo "║              sudo journalctl -u gsserver -f                    ║"
echo "║                                                                 ║"
echo "║  Next: Run ./scripts/06-test-hardening.sh                     ║"
echo "║                                                                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
