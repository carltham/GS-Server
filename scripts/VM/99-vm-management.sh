#!/bin/bash
# Utility script for VM management (start, stop, reset, etc.)

COMMAND="${1:-help}"

VMs=("dev-base" "test-baseline" "test-hardened" "test-isolated")

show_help() {
  cat << EOF
VM Management Utility

Usage: $0 <command> [options]

Commands:
  start              Start all VMs
  stop               Stop all VMs
  kill               Force stop all VMs
  restart            Restart all VMs
  status             Show VM status
  list               List all VMs

  start-vm <name>    Start specific VM
  stop-vm <name>     Stop specific VM
  ssh <name>         SSH to VM

  logs <name>        Show application logs
  cleanup            Delete all VMs

Examples:
  $0 start
  $0 ssh test-baseline
  $0 logs test-hardened

EOF
}

start_all() {
  echo "Starting all VMs..."
  for vm in "${VMs[@]}"; do
    if ! sudo virsh list 2>/dev/null | grep -q "^ *$vm "; then
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
    if sudo virsh list 2>/dev/null | grep -q "^ *$vm "; then
      echo "  Stopping $vm..."
      sudo virsh shutdown "$vm" 2>/dev/null || true
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
  sleep 3
  start_all
}

status_vms() {
  echo "VM Status:"
  sudo virsh list --all
}

list_vms() {
  echo "GS-Server Test VMs:"
  IPs=("192.168.100.10" "192.168.100.11" "192.168.100.12" "192.168.100.13")
  for i in "${!VMs[@]}"; do
    printf "  %-20s %s\n" "${VMs[$i]}" "${IPs[$i]}"
  done
}

start_vm() {
  local vm_name="$1"
  [ -z "$vm_name" ] && { echo "Usage: $0 start-vm <name>"; return 1; }
  echo "Starting $vm_name..."
  sudo virsh start "$vm_name"
}

stop_vm() {
  local vm_name="$1"
  [ -z "$vm_name" ] && { echo "Usage: $0 stop-vm <name>"; return 1; }
  echo "Stopping $vm_name..."
  sudo virsh shutdown "$vm_name"
}

ssh_vm() {
  local vm_name="$1"
  [ -z "$vm_name" ] && { echo "Usage: $0 ssh <name>"; return 1; }

  IPs=("192.168.100.10" "192.168.100.11" "192.168.100.12" "192.168.100.13")
  for i in "${!VMs[@]}"; do
    if [ "${VMs[$i]}" = "$vm_name" ]; then
      ssh "ubuntu@${IPs[$i]}"
      return
    fi
  done
  echo "❌ VM not found: $vm_name"
}

show_logs() {
  local vm_name="$1"
  [ -z "$vm_name" ] && { echo "Usage: $0 logs <name>"; return 1; }

  IPs=("192.168.100.10" "192.168.100.11" "192.168.100.12" "192.168.100.13")
  for i in "${!VMs[@]}"; do
    if [ "${VMs[$i]}" = "$vm_name" ]; then
      ssh "ubuntu@${IPs[$i]}" "sudo journalctl -u gsserver -n 50 --no-pager" || echo "(Service not running)"
      return
    fi
  done
  echo "❌ VM not found: $vm_name"
}

cleanup() {
  echo "⚠️  This will delete all VMs!"
  read -p "Are you sure? (type 'yes' to confirm): " confirmation
  [ "$confirmation" != "yes" ] && { echo "Cancelled"; return 0; }

  echo "Deleting VMs..."
  for vm in "${VMs[@]}"; do
    sudo virsh destroy "$vm" 2>/dev/null || true
    sudo virsh undefine "$vm" --remove-all-storage 2>/dev/null || true
  done
  echo "✅ Cleanup complete"
}

# Execute command
case "$COMMAND" in
  help) show_help ;;
  start) start_all ;;
  stop) stop_all ;;
  kill) kill_all ;;
  restart) restart_all ;;
  status) status_vms ;;
  list) list_vms ;;
  start-vm) start_vm "$2" ;;
  stop-vm) stop_vm "$2" ;;
  ssh) ssh_vm "$2" ;;
  logs) show_logs "$2" ;;
  cleanup) cleanup ;;
  *) echo "Unknown command: $COMMAND"; show_help; exit 1 ;;
esac
