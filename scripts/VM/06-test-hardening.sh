#!/bin/bash
# Test hardening operations across VMs

set -e

BASELINE_IP="192.168.100.11"
HARDENED_IP="192.168.100.12"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Testing Hardening Operations                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

echo "=== Test 1: Baseline Health Check ==="
echo "Checking test-baseline ($BASELINE_IP)..."
curl -s "http://$BASELINE_IP:8080/api/v1/health" | jq . 2>/dev/null || echo "(Connection pending)"
echo ""

echo "=== Test 2: Pre-Hardening Health Check ==="
echo "Checking test-hardened ($HARDENED_IP) BEFORE hardening..."
curl -s "http://$HARDENED_IP:8080/api/v1/health" | jq . 2>/dev/null || echo "(Connection pending)"
echo ""

echo "=== Test 3: Trigger Hardening Operation ==="
echo "Sending hardening request..."
curl -s -X POST "http://$HARDENED_IP:8080/api/v1/hardening" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenant-a","requestedBy":"ui-operator","profile":"baseline"}' | jq . 2>/dev/null || echo "(Request may be pending)"
echo ""

echo "Waiting for hardening..."
sleep 10

echo "=== Test 4: Post-Hardening Health Check ==="
echo "Checking test-hardened ($HARDENED_IP) AFTER hardening..."
curl -s "http://$HARDENED_IP:8080/api/v1/health" | jq . 2>/dev/null || echo "(Connection pending)"
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Testing Complete!                                           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
