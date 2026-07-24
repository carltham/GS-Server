package com.gsserver.ui.proxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProxyInstallationService {
  private static final Logger logger = LoggerFactory.getLogger(ProxyInstallationService.class);
  private static final Path SITE_FILE_PATH =
      Path.of("/etc/nginx/sites-available/gsserver-proxy");

  private final ServerPortHolder serverPortHolder;

  public ProxyInstallationService(ServerPortHolder serverPortHolder) {
    this.serverPortHolder = serverPortHolder;
  }

  /**
   * Return the reverse-proxy site file for editing: the existing content when the file is present
   * and readable, otherwise a valid template that proxies to this application.
   */
  public SiteFileResponse getSiteFile() {
    if (Files.isRegularFile(SITE_FILE_PATH)) {
      try {
        String content = Files.readString(SITE_FILE_PATH);
        return new SiteFileResponse(SITE_FILE_PATH.toString(), true, content);
      } catch (IOException e) {
        // File exists but the server user cannot read it (e.g. restrictive perms); offer the
        // template so the operator can still (re)write it through the terminal.
        logger.info("Site file exists but could not be read: {}", e.getMessage());
        return new SiteFileResponse(SITE_FILE_PATH.toString(), true, defaultSiteTemplate());
      }
    }
    return new SiteFileResponse(SITE_FILE_PATH.toString(), false, defaultSiteTemplate());
  }

  /** A valid nginx reverse-proxy config that fronts this application (incl. WebSocket upgrade). */
  private String defaultSiteTemplate() {
    int port = serverPortHolder.getPort();
    return """
        # Reverse proxy for the GSServer UI (this application)
        server {
            listen 80;
            server_name _;

            location /serveradmin {
                proxy_pass http://127.0.0.1:%d;

                # Preserve client information
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;

                # Required for the interactive terminal WebSocket
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_read_timeout 3600s;
            }
        }
        """
        .formatted(port);
  }

  public ProxyInstallGuideResponse getInstallGuide() {
    String osName = System.getProperty("os.name", "unknown");
    String osLower = osName.toLowerCase(Locale.ROOT);

    if (osLower.contains("linux")) {
      return new ProxyInstallGuideResponse(
          "Linux",
          List.of(
              "Install NGINX or Apache using your distro package manager.",
              "Enable the service at boot and start it now.",
              "Check service status and confirm the binary path.",
              "Return to Proxy Management and re-run detection."),
          linuxCommands());
    }

    if (osLower.contains("win")) {
      return new ProxyInstallGuideResponse(
          "Windows",
          List.of(
              "Install NGINX or Apache using the official installer or package manager.",
              "Run the service as Administrator.",
              "Verify the process is running before returning to Proxy Management."),
          List.of("echo Windows install is not automated in this terminal runner."));
    }

    if (osLower.contains("mac")) {
      return new ProxyInstallGuideResponse(
          "macOS",
          List.of(
              "Install NGINX or Apache using Homebrew.",
              "Start the service and verify process status.",
              "Return to Proxy Management and re-run detection."),
          List.of("echo macOS install is not automated in this terminal runner."));
    }

    return new ProxyInstallGuideResponse(
        osName,
        List.of("Unsupported operating system for guided proxy installation."),
        List.of());
  }

  private List<String> linuxCommands() {
    List<String> commands = new ArrayList<>();
    // 1. Install NGINX
    commands.add("cat /etc/os-release");
    commands.add("which apt");
    commands.add("sudo apt update");
    commands.add("sudo apt install -y nginx");
    commands.add("sudo systemctl enable nginx");
    commands.add("sudo systemctl start nginx");
    commands.add("sudo systemctl status nginx");
    // 2. Enable and reload the reverse-proxy site (after creating the config, see the guide)
    commands.add("sudo ln -sf /etc/nginx/sites-available/gsserver-proxy /etc/nginx/sites-enabled/gsserver-proxy");
    commands.add("sudo nginx -t");
    commands.add("sudo systemctl reload nginx");
    return commands;
  }
}
