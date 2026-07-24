package com.gsserver.ui.hardening.adapter;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WindowsHardeningAdapter {
  private final HardeningCommandExecutor commandExecutor;

  public WindowsHardeningAdapter(HardeningCommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public HardeningExecutionReport applyBaselineHardening() {
    String script = """
        Write-Host "=== Windows Baseline Hardening ===" 
        $ErrorActionPreference = "SilentlyContinue"
        
        # Enable Windows Firewall for all profiles
        Write-Host "[+] Enabling Windows Firewall..."
        Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled $true
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultInboundAction Block
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultOutboundAction Allow
        
        # Allow RDP (3389) for remote access
        New-NetFirewallRule -DisplayName "Allow RDP" -Direction Inbound -LocalPort 3389 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue
        
        # Enable Windows Defender
        Write-Host "[+] Configuring Windows Defender..."
        if (Get-Command Get-MpPreference -ErrorAction SilentlyContinue) {
            Set-MpPreference -DisableRealtimeMonitoring $false
            Set-MpPreference -DisableBehaviorMonitoring $false
            Set-MpPreference -DisableIntrusionPreventionSystem $false
        }
        
        # Disable unnecessary services
        Write-Host "[+] Disabling unnecessary services..."
        @("Fax", "XboxLiveAuthManager", "XboxLiveGameSave") | ForEach-Object {
            $svc = Get-Service $_ -ErrorAction SilentlyContinue
            if ($svc) {
                Set-Service $_ -StartupType Disabled
                Stop-Service $_ -Force -ErrorAction SilentlyContinue
            }
        }
        
        # Enable Windows Update
        Write-Host "[+] Enabling Windows Update..."
        Set-Service wuauserv -StartupType Automatic
        Start-Service wuauserv -ErrorAction SilentlyContinue
        
        # Configure account lockout policy
        Write-Host "[+] Configuring account lockout policy..."
        net accounts /lockoutduration:30 /lockoutthreshold:5 /lockoutwindow:30
        
        # Enable audit logging
        Write-Host "[+] Enabling audit logging..."
        auditpol /set /subcategory:"Logon/Logoff" /success:enable /failure:enable
        
        Write-Host "=== Baseline hardening completed successfully ===" 
        """;

    List<String> command =
        List.of(
            "powershell",
            "-NoProfile",
            "-Command",
            script);

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "windows", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport applyStrictHardening() {
    String script = """
        Write-Host "=== Windows Strict Hardening ===" 
        $ErrorActionPreference = "SilentlyContinue"
        
        # Enhanced firewall rules
        Write-Host "[+] Configuring strict firewall rules..."
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultInboundAction Block
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultOutboundAction Block
        
        # Allow only essential outbound (DNS, HTTPS)
        New-NetFirewallRule -DisplayName "Allow DNS" -Direction Outbound -RemotePort 53 -Protocol UDP -Action Allow -ErrorAction SilentlyContinue
        New-NetFirewallRule -DisplayName "Allow HTTPS" -Direction Outbound -RemotePort 443 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue
        New-NetFirewallRule -DisplayName "Allow HTTP" -Direction Outbound -RemotePort 80 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue
        
        # Disable SMB v1
        Write-Host "[+] Disabling SMB v1..."
        Disable-WindowsOptionalFeature -Online -FeatureName SMB1Protocol -NoRestart -ErrorAction SilentlyContinue
        
        # Disable unnecessary features
        Write-Host "[+] Disabling unnecessary features..."
        @("Internet-Explorer-Optional-amd64", "RasDial", "Xps-Foundation-Xps-Viewer") | ForEach-Object {
            Disable-WindowsOptionalFeature -Online -FeatureName $_ -NoRestart -ErrorAction SilentlyContinue
        }
        
        # Configure password policy
        Write-Host "[+] Configuring password policy..."
        net accounts /minpwlen:14 /maxpwage:90 /uniquepw:5
        
        # Disable unnecessary services (strict)
        Write-Host "[+] Disabling unnecessary services..."
        @("lfsvc", "MapsBroker", "DiagTrack", "dmwappushservice") | ForEach-Object {
            Set-Service $_ -StartupType Disabled -ErrorAction SilentlyContinue
            Stop-Service $_ -Force -ErrorAction SilentlyContinue
        }
        
        # Enable enhanced audit logging
        Write-Host "[+] Enabling enhanced audit logging..."
        auditpol /set /subcategory:"Process Creation" /success:enable /failure:enable
        auditpol /set /subcategory:"File Share" /success:enable /failure:enable
        auditpol /set /subcategory:"Registry" /success:enable /failure:enable
        
        Write-Host "=== Strict hardening completed successfully ===" 
        """;

    List<String> command =
        List.of(
            "powershell",
            "-NoProfile",
            "-Command",
            script);

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "windows", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport rollbackBaselineHardening() {
    String script = """
        Write-Host "=== Rolling back Windows Baseline Hardening ===" 
        $ErrorActionPreference = "SilentlyContinue"
        
        # Disable Windows Firewall
        Write-Host "[+] Disabling Windows Firewall..."
        Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled $false
        
        # Remove firewall rules we added
        Write-Host "[+] Removing custom firewall rules..."
        Remove-NetFirewallRule -DisplayName "Allow RDP" -ErrorAction SilentlyContinue
        
        # Re-enable disabled services
        Write-Host "[+] Re-enabling services..."
        @("Fax", "XboxLiveAuthManager", "XboxLiveGameSave") | ForEach-Object {
            Set-Service $_ -StartupType Automatic -ErrorAction SilentlyContinue
            Start-Service $_ -ErrorAction SilentlyContinue
        }
        
        Write-Host "=== Baseline rollback completed successfully ===" 
        """;

    List<String> command =
        List.of(
            "powershell",
            "-NoProfile",
            "-Command",
            script);

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "windows", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }

  public HardeningExecutionReport rollbackStrictHardening() {
    String script = """
        Write-Host "=== Rolling back Windows Strict Hardening ===" 
        $ErrorActionPreference = "SilentlyContinue"
        
        # Remove strict firewall rules
        Write-Host "[+] Removing strict firewall rules..."
        Remove-NetFirewallRule -DisplayName "Allow DNS" -ErrorAction SilentlyContinue
        Remove-NetFirewallRule -DisplayName "Allow HTTPS" -ErrorAction SilentlyContinue
        Remove-NetFirewallRule -DisplayName "Allow HTTP" -ErrorAction SilentlyContinue
        
        # Reset firewall to defaults (inbound block, outbound allow)
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultInboundAction Block
        Set-NetFirewallProfile -Profile Domain,Public,Private -DefaultOutboundAction Allow
        
        # Re-enable SMB v1
        Write-Host "[+] Re-enabling SMB v1..."
        Enable-WindowsOptionalFeature -Online -FeatureName SMB1Protocol -NoRestart -ErrorAction SilentlyContinue
        
        # Re-enable services
        Write-Host "[+] Re-enabling services..."
        @("lfsvc", "MapsBroker", "DiagTrack") | ForEach-Object {
            Set-Service $_ -StartupType Automatic -ErrorAction SilentlyContinue
            Start-Service $_ -ErrorAction SilentlyContinue
        }
        
        Write-Host "=== Strict hardening rollback completed successfully ===" 
        """;

    List<String> command =
        List.of(
            "powershell",
            "-NoProfile",
            "-Command",
            script);

    CommandExecutionResult result = commandExecutor.execute(command, Duration.ofMinutes(2));
    return new HardeningExecutionReport(
        "windows", result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
  }
}
