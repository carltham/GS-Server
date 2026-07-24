#!/bin/bash
# Utility script for VM management (start, stop, reset, etc.)

COMMAND="${1:-help}"

VMs=("dev-base" "test-baseline" "test-hardened" "test-isolated")
VM_IPS=(
  "192.168.100.10:dev-base"
  "192.168.100.11:test-baseline"
  "192.168.100.12:test-hardened"
  "192.168.100.13:test-isolated"
)

show_help() {
  cat << EOF
VM Management Utility

Usage: $0 <command> [options]

Commands:
  start              Start all VMs
  stop               Stop all VMs gracefully
  kill               Force stop all VMs
  restart            Restart all VMs
  status             Show VM status
  list               List all VMs with IPs

  start-vm <name>    Start specific VM
  stop-vm <name>     Stop specific VM

  ssh <name>         SSH to VM (as deploy user)
  console <name>     Open VM console

  snapshot <name>    Take snapshot of VM
  restore <name> <snapshot>  Restore snapshot

  logs <name>        Show application logs
  audit-log <name>   Show audit logs

  cleanup            Remove all VMs and snapshots

Examples:
  $0 start                    # Start all VMs
  $0 ssh test-baseline        # SSH to test-baseline
  $0 logs test-hardened       # Show logs from test-hardened
  $0 snapshot test-baseline   # Take snapshot before test

EOF
}

start_all() {
  echo "Starting all VMs..."
  for vm in "${VMs[@]}"; do
    if ! sudo virsh list | grep -q "^ *$vm "; then
      echo "  Starting $vm..."
      sudo virsh start "$vm"
    else
      echo "  $vm already running"
    fi
  done
  echo "✅ All VMs started"
}

stop_all() {
  echo "Stopping all VMs..."
  for vm in "${VMs[@]}"; do
    if sudo virsh list | grep -q "^ *$vm "; then
      echo "  Stopping $vm..."
      sudo virsh shutdown "$vm"
    else
      echo "  $vm already stopped"
    fi
  done
  echo "✅ All VMs stopped"
}

kill_all() {
  echo "Force stopping all VMs..."
  for vm in "${VMs[@]}"; do
    sudo virsh destroy "$vm" 2>/dev/null || true
  done
  echo "✅ All VMs force-stopped"
}

restart_all() {
  stop_all
  sleep 5
  start_all
}

status_vms() {
  echo "VM Status:"
  sudo virsh list --all
  echo ""
  echo "Network Status:"
  for vm_config in "${VM_IPS[@]}"; do
    IFS=':' read -r vm_ip vm_name <<< "$vm_config"
    if ping -c 1 "$vm_ip" &>/dev/null; then
      echo "  $vm_name ($vm_ip): ✅ Reachable"
    else
      echo "  $vm_name ($vm_ip): ❌ Unreachable"
    fi
  done
}

list_vms() {
  echo "GS-Server Test VMs:"
  for vm_config in "${VM_IPS[@]}"; do
    IFS=':' read -r vm_ip vm_name <<< "$vm_config"
    status=$(sudo virsh list --all | grep "^ *$vm_name" | awk '{print $3}' || echo "stopped")
    printf "  %-20s %s  (%s)\n" "$vm_name" "$vm_ip" "$status"
  done
  echo ""
  echo "SSH: ssh deploy@<ip>  (password: deploy-password)"
}

start_vm() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 start-vm <name>"
    return 1
  fi

  echo "Starting $vm_name..."
  sudo virsh start "$vm_name"
  echo "✅ Done"
}

stop_vm() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 stop-vm <name>"
    return 1
  fi

  echo "Stopping $vm_name..."
  sudo virsh shutdown "$vm_name"
  echo "✅ Done"
}

ssh_vm() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 ssh <name>"
    return 1
  fi

  # Find IP for VM
  local vm_ip=""
  for vm_config in "${VM_IPS[@]}"; do
    IFS=':' read -r ip name <<< "$vm_config"
    if [ "$name" = "$vm_name" ]; then
      vm_ip="$ip"
      break
    fi
  done

  if [ -z "$vm_ip" ]; then
    echo "❌ VM not found: $vm_name"
    return 1
  fi

  echo "Connecting to $vm_name ($vm_ip)..."
  ssh "deploy@$vm_ip"
}

console_vm() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 console <name>"
    return 1
  fi

  echo "Opening console for $vm_name (press Ctrl+] to exit)..."
  sudo virsh console "$vm_name"
}

snapshot_vm() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 snapshot <name>"
    return 1
  fi

  local snapshot_name="manual-$(date +%s)"
  echo "Creating snapshot '$snapshot_name' for $vm_name..."
  sudo virsh snapshot-create-as "$vm_name" "$snapshot_name" "Manual snapshot"
  echo "✅ Snapshot created"
}

restore_snapshot() {
  local vm_name="$1"
  local snapshot_name="$2"

  if [ -z "$vm_name" ] || [ -z "$snapshot_name" ]; then
    echo "Usage: $0 restore <vm-name> <snapshot-name>"
    return 1
  fi

  echo "Restoring $vm_name to snapshot '$snapshot_name'..."
  sudo virsh snapshot-revert "$vm_name" "$snapshot_name"
  echo "✅ Snapshot restored"
}

show_logs() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 logs <name>"
    return 1
  fi

  # Find IP for VM
  local vm_ip=""
  for vm_config in "${VM_IPS[@]}"; do
    IFS=':' read -r ip name <<< "$vm_config"
    if [ "$name" = "$vm_name" ]; then
      vm_ip="$ip"
      break
    fi
  done

  if [ -z "$vm_ip" ]; then
    echo "❌ VM not found: $vm_name"
    return 1
  fi

  echo "Application logs from $vm_name:"
  ssh "deploy@$vm_ip" "sudo journalctl -u gsserver -n 50 --no-pager" || echo "(Service not running)"
}

show_audit_log() {
  local vm_name="$1"
  if [ -z "$vm_name" ]; then
    echo "Usage: $0 audit-log <name>"
    return 1
  fi

  # Find IP for VM
  local vm_ip=""
  for vm_config in "${VM_IPS[@]}"; do
    IFS=':' read -r ip name <<< "$vm_config"
    if [ "$name" = "$vm_name" ]; then
      vm_ip="$ip"
      break
    fi
  done

  if [ -z "$vm_ip" ]; then
    echo "❌ VM not found: $vm_name"
    return 1
  fi

  echo "Audit logs from $vm_name:"
  ssh "deploy@$vm_ip" "sudo tail -50 /var/log/gsserver/audit.log" || echo "(Audit log not found)"
}

cleanup() {
  echo "⚠️  This will delete all VMs and snapshots!"
  read -p "Are you sure? (type 'yes' to confirm): " confirmation

  if [ "$confirmation" != "yes" ]; then
    echo "Cancelled"
    return 0
  fi

  echo "Deleting VMs..."
  for vm in "${VMs[@]}"; do
    echo "  Deleting $vm..."
    sudo virsh destroy "$vm" 2>/dev/null || true
    sudo virsh undefine "$vm" --remove-all-storage 2>/dev/null || true
  done

  echo "Deleting disk images..."
  for vm in "${VMs[@]}"; do
    sudo rm -f "/mnt/STORAGE/VM_KVM/$vm.qcow2"
  done

  echo "✅ Cleanup complete"
}

# Execute command
case "$COMMAND" in
  help)
    show_help
    ;;
  start)
    start_all
    ;;
  stop)
    stop_all
    ;;
  kill)
    kill_all
    ;;
  restart)
    restart_all
    ;;
  status)
    status_vms
    ;;
  list)
    list_vms
    ;;
  start-vm)
    start_vm "$2"
    ;;
  stop-vm)
    stop_vm "$2"
    ;;
  ssh)
    ssh_vm "$2"
    ;;
  console)
    console_vm "$2"
    ;;
  snapshot)
    snapshot_vm "$2"
    ;;
  restore)
    restore_snapshot "$2" "$3"
    ;;
  logs)
    show_logs "$2"
    ;;
  audit-log)
    show_audit_log "$2"
    ;;
  cleanup)
    cleanup
    ;;
  *)
    echo "Unknown command: $COMMAND"
    echo ""
    show_help
    exit 1
    ;;
esac
