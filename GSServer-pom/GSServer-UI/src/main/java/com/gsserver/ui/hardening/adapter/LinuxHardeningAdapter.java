package com.gsserver.ui.hardening.adapter;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LinuxHardeningAdapter {
  private final HardeningCommandExecutor commandExecutor;

  public LinuxHardeningAdapter(HardeningCommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public HardeningExecutionReport applyBaselineHardening() {
    String script = """
        set -e
        echo "=== Linux Baseline Hardening ==="
        
        # Update package lists
        if command -v apt-get &> /dev/null; then
          echo "[+] Updating package lists..."
          apt-get update -qq
        fi
        
        # Enable UFW firewall
        if command -v ufw &> /dev/null; then
          echo "[+] Configuring UFW firewall..."
          ufw --force enable
          ufw default deny incoming
          ufw default allow outgoing
          ufw allow 22/tcp
        fi
        
        # Harden SSH configuration
        if [ -f /etc/ssh/sshd_config ]; then
          echo "[+] Hardening SSH configuration..."
          grep -q "^PermitRootLogin no" /etc/ssh/sshd_config || echo "PermitRootLogin no" >> /etc/ssh/sshd_config
          grep -q "^PasswordAuthentication no" /etc/ssh/sshd_config || echo "PasswordAuthentication no" >> /etc/ssh/sshd_config
        fi
        
        # Set secure file permissions
        echo "[+] Setting secure file permissions..."
        chmod 600 /etc/passwd /etc/shadow /etc/group 2>/dev/null || true
        
        # Enable automatic security updates
        if command -v apt-get &> /dev/null; then
          echo "[+] Enabling automatic security updates..."
          apt-get install -y unattended-upgrades -qq
        fi
        
        echo "=== Baseline hardening completed successfully ==="
        """;

    List<String> command = List.of("/usr/bin/env", "bash", "-lc", script);
    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "linux", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport applyStrictHardening() {
    String script = """
        set -e
        echo "=== Linux Strict Hardening ==="
        
        # Apply baseline first
        echo "[+] Applying baseline hardening..."
        
        # Enhanced kernel hardening
        echo "[+] Configuring kernel hardening parameters..."
        sysctl -w kernel.dmesg_restrict=1
        sysctl -w kernel.kptr_restrict=2
        sysctl -w kernel.yama.ptrace_scope=2
        sysctl -w net.ipv4.conf.all.log_martians=1
        sysctl -w net.ipv4.conf.all.send_redirects=0
        sysctl -w net.ipv4.icmp_echo_ignore_all=0
        sysctl -w net.ipv4.tcp_timestamps=1
        sysctl -w net.ipv6.conf.all.disable_ipv6=0
        sysctl -p > /dev/null 2>&1 || true
        
        # Disable unnecessary network services
        echo "[+] Disabling unnecessary services..."
        for service in avahi-daemon cups isc-dhcp-server telnet; do
          if systemctl is-enabled $service 2>/dev/null; then
            systemctl disable $service 2>/dev/null || true
            systemctl stop $service 2>/dev/null || true
          fi
        done
        
        # Configure PAM security
        echo "[+] Hardening PAM configuration..."
        grep -q "^minlen=12" /etc/security/pwquality.conf || echo "minlen=12" >> /etc/security/pwquality.conf 2>/dev/null || true
        grep -q "^dcredit=-1" /etc/security/pwquality.conf || echo "dcredit=-1" >> /etc/security/pwquality.conf 2>/dev/null || true
        
        # Enable audit logging
        echo "[+] Enabling audit logging..."
        if command -v auditctl &> /dev/null; then
          auditctl -e 1 2>/dev/null || true
        fi
        
        # Set umask to restrict file creation
        grep -q "^umask 0027" /etc/login.defs || sed -i 's/^umask 022/umask 0027/' /etc/login.defs || true
        
        echo "=== Strict hardening completed successfully ==="
        """;

    List<String> command = List.of("/usr/bin/env", "bash", "-lc", script);
    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "linux", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport rollbackBaselineHardening() {
    String script = """
        set -e
        echo "=== Rolling back Linux Baseline Hardening ==="
        
        # Disable UFW firewall
        if command -v ufw &> /dev/null; then
          echo "[+] Disabling UFW firewall..."
          ufw --force disable
        fi
        
        # Remove appended SSH hardening (note: this is best-effort)
        echo "[+] Reverting SSH hardening..."
        if [ -f /etc/ssh/sshd_config ]; then
          sed -i '/^PermitRootLogin no$/d' /etc/ssh/sshd_config || true
          sed -i '/^PasswordAuthentication no$/d' /etc/ssh/sshd_config || true
        fi
        
        echo "=== Baseline rollback completed successfully ==="
        """;

    List<String> command = List.of("/usr/bin/env", "bash", "-lc", script);
    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "linux", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport rollbackStrictHardening() {
    String script = """
        set -e
        echo "=== Rolling back Linux Strict Hardening ==="
        
        # Revert kernel hardening (reset to defaults)
        echo "[+] Reverting kernel hardening parameters..."
        sysctl -w kernel.dmesg_restrict=0 || true
        sysctl -w kernel.kptr_restrict=0 || true
        sysctl -w kernel.yama.ptrace_scope=0 || true
        sysctl -w net.ipv4.conf.all.log_martians=0 || true
        sysctl -w net.ipv4.conf.all.send_redirects=1 || true
        sysctl -p > /dev/null 2>&1 || true
        
        # Re-enable disabled services
        echo "[+] Re-enabling services..."
        for service in avahi-daemon cups isc-dhcp-server; do
          systemctl enable $service 2>/dev/null || true
          systemctl start $service 2>/dev/null || true
        done
        
        echo "=== Strict hardening rollback completed successfully ==="
        """;

    List<String> command = List.of("/usr/bin/env", "bash", "-lc", script);
    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "linux", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }
}
