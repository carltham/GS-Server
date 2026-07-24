#!/bin/bash
# Test hardening operations across VMs

set -e

# VM IPs
BASELINE_IP="192.168.100.11"
HARDENED_IP="192.168.100.12"

API_TOKEN="test-token"  # Replace with real token from application

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Testing Hardening Operations                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Helper function to test API
test_api() {
  local method="$1"
  local url="$2"
  local data="$3"
  local vm_ip="$4"

  echo "  API Call: $method $url"

  if [ -n "$data" ]; then
    curl -s -X "$method" \
      -H "Authorization: Bearer $API_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "http://$vm_ip:8080$url" | jq . 2>/dev/null || echo "  (Connection error or invalid response)"
  else
    curl -s -X "$method" \
      -H "Authorization: Bearer $API_TOKEN" \
      "http://$vm_ip:8080$url" | jq . 2>/dev/null || echo "  (Connection error or invalid response)"
  fi
  echo ""
}

# Test 1: Baseline health check
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 1: Baseline Health Check"
echo "═══════════════════════════════════════════════════════════════════"
echo "Checking test-baseline ($BASELINE_IP)..."
echo ""

test_api GET "/api/v1/health" "" "$BASELINE_IP"

# Test 2: Hardened health check (before hardening)
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 2: Pre-Hardening Health Check"
echo "═══════════════════════════════════════════════════════════════════"
echo "Checking test-hardened ($HARDENED_IP) BEFORE hardening..."
echo ""

test_api GET "/api/v1/health" "" "$HARDENED_IP"

# Record pre-hardening metrics
echo "Recording pre-hardening metrics..."
BASELINE_BEFORE=$(curl -s "http://$BASELINE_IP:8080/api/v1/metrics" 2>/dev/null | jq .uptime 2>/dev/null || echo "unknown")
HARDENED_BEFORE=$(curl -s "http://$HARDENED_IP:8080/api/v1/metrics" 2>/dev/null | jq .uptime 2>/dev/null || echo "unknown")

echo "  Baseline uptime: $BASELINE_BEFORE"
echo "  Hardened uptime (before): $HARDENED_BEFORE"
echo ""

# Test 3: Trigger hardening
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 3: Trigger Hardening Operation"
echo "═══════════════════════════════════════════════════════════════════"
echo "Sending hardening request to test-hardened ($HARDENED_IP)..."
echo ""

HARDENING_DATA='{
  "tenantId": "tenant-a",
  "requestedBy": "ui-operator",
  "profile": "baseline"
}'

test_api POST "/api/v1/hardening" "$HARDENING_DATA" "$HARDENED_IP"

# Wait for hardening to complete
echo "Waiting for hardening operation to complete..."
sleep 10

# Test 4: Post-hardening health check
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 4: Post-Hardening Health Check"
echo "═══════════════════════════════════════════════════════════════════"
echo "Checking test-hardened ($HARDENED_IP) AFTER hardening..."
echo ""

test_api GET "/api/v1/health" "" "$HARDENED_IP"

# Test 5: Get operation state
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 5: Get Hardening Operation State"
echo "═══════════════════════════════════════════════════════════════════"

test_api GET "/api/v1/hardening/latest" "" "$HARDENED_IP"

# Test 6: Compare metrics
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 6: Performance Comparison"
echo "═══════════════════════════════════════════════════════════════════"

HARDENED_AFTER=$(curl -s "http://$HARDENED_IP:8080/api/v1/metrics" 2>/dev/null | jq .uptime 2>/dev/null || echo "unknown")
echo "  Baseline uptime: $BASELINE_BEFORE"
echo "  Hardened uptime (after): $HARDENED_AFTER"
echo ""

# Test 7: Security audit
echo "═══════════════════════════════════════════════════════════════════"
echo "Test 7: Security Audit (requires SSH access)"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

echo "Open ports on BASELINE:"
ssh -q deploy@$BASELINE_IP "sudo ss -tuln 2>/dev/null | grep LISTEN | wc -l" || echo "  (SSH error)"

echo ""
echo "Open ports on HARDENED (after hardening):"
ssh -q deploy@$HARDENED_IP "sudo ss -tuln 2>/dev/null | grep LISTEN | wc -l" || echo "  (SSH error)"

echo ""
echo "Firewall rules on BASELINE:"
ssh -q deploy@$BASELINE_IP "sudo iptables -L 2>/dev/null | grep -c DROP || echo 0" || echo "  (SSH error)"

echo ""
echo "Firewall rules on HARDENED:"
ssh -q deploy@$HARDENED_IP "sudo iptables -L 2>/dev/null | grep -c DROP || echo 0" || echo "  (SSH error)"

echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Testing Complete!                                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║                                                                 ║"
echo "║  Next Steps:                                                    ║"
echo "║    1. Review test results above                                ║"
echo "║    2. Compare baseline vs hardened performance                 ║"
echo "║    3. Check security audit differences                         ║"
echo "║    4. Run cross-tenant tests on test-isolated                 ║"
echo "║                                                                 ║"
echo "║  Debug Tips:                                                    ║"
echo "║    ssh deploy@192.168.100.11 (test-baseline)                  ║"
echo "║    ssh deploy@192.168.100.12 (test-hardened)                  ║"
echo "║                                                                 ║"
echo "║    sudo journalctl -u gsserver -f (app logs)                  ║"
echo "║    tail -f /var/log/gsserver/audit.log (audit log)            ║"
echo "║                                                                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
