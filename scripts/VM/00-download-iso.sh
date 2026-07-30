#!/bin/bash
# Download Ubuntu ISO for base VM creation

set -e

ISO_PATH="${1:-/mnt/STORAGE/iso/ubuntu-24.04.4-live-server-amd64.iso}"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Downloading Ubuntu ISO                                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if ISO already exists
if [ -f "$ISO_PATH" ]; then
  echo "✅ ISO already exists at $ISO_PATH"
  echo ""
  echo "Next: Run ./00-create-base-vm.sh"
  exit 0
fi

echo "Downloading Ubuntu 24.04.3 ISO..."
echo "Target: $ISO_PATH"
echo ""

mkdir -p "$ISO_DIR"

wget https://releases.ubuntu.com/24.04/ubuntu-24.04.3-live-server-amd64.iso -O "$ISO_PATH"

echo ""
echo "✅ ISO downloaded successfully!"
echo ""
echo "Size: $(du -h "$ISO_PATH" | cut -f1)"
echo ""
echo "Next: Run ./00-create-base-vm.sh"
