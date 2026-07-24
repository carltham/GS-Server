import { Component, ViewChild } from '@angular/core';

import {
  ProxyInstallGuideResponse,
  ProxyInstallationApiService
} from './proxy-installation-api.service';
import { WebTerminalComponent } from './web-terminal.component';

@Component({
  selector: 'gs-proxy-installation',
  templateUrl: './proxy-installation.component.html',
  styleUrls: ['./proxy-installation.component.scss']
})
export class ProxyInstallationComponent {
  @ViewChild(WebTerminalComponent) terminal?: WebTerminalComponent;

  // Commands that belong to the "Configure the reverse proxy" step; everything else is install.
  private static readonly PROXY_COMMAND_MARKERS = [
    'sites-enabled',
    'gsserver-proxy',
    'nginx -t',
    'reload nginx'
  ];

  static readonly SITE_FILE_PATH = '/etc/nginx/sites-available/gsserver-proxy';

  // Same POSIX-username shape the backend enforces at the WebSocket handshake.
  private static readonly USERNAME_PATTERN = /^[a-z_][a-z0-9_-]{0,31}$/;

  // Up-front sudo authentication. The marker is printed only on success and is split in the command
  // text (printf 'GS_%s_OK' SUDO) so the echoed command line itself never matches it.
  private static readonly SUDO_SENTINEL = 'GS_SUDO_OK';
  private static readonly SUDO_AUTH_COMMAND = "sudo -v && printf 'GS_%s_OK\\n' SUDO";

  private static readonly DEFAULT_SITE_CONFIG = `# Reverse proxy for the GSServer UI (this application)
server {
    listen 80;
    server_name _;

    location /serveradmin {
        proxy_pass http://127.0.0.1:8080;

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
`;

  guide: ProxyInstallGuideResponse | null = null;
  guideError = '';

  terminalVisible = false;
  terminalBlocked = false;
  terminalReady = false;
  sentCommands = new Set<string>();
  activeCommandGroup: 'all' | 'install' | 'proxy' = 'all';
  // OS user the shell runs as. Preselected to root; leave blank to use the server's own user.
  terminalUser = 'root';

  readonly siteFilePath = ProxyInstallationComponent.SITE_FILE_PATH;

  // Labeled activation commands shown after the button panel in configuration mode.
  readonly proxyConfigSteps: { label: string; command: string }[] = [
    {
      label: 'Enable the site',
      command: `sudo ln -sf ${ProxyInstallationComponent.SITE_FILE_PATH} /etc/nginx/sites-enabled/gsserver-proxy`
    },
    { label: 'Validate the configuration', command: 'sudo nginx -t' },
    { label: 'Apply it without downtime', command: 'sudo systemctl reload nginx' }
  ];

  editorVisible = false;
  editorContent = ProxyInstallationComponent.DEFAULT_SITE_CONFIG;
  editorLoading = false;
  editorFileExists = false;
  editorStatus = '';
  // The editor stays locked until the user authenticates sudo in the terminal (up front).
  sudoAuthenticated = false;
  sudoAuthPending = false;

  terminalLeft = 120;
  terminalTop = 120;
  editorLeft = 220;
  editorTop = 90;

  constructor(private readonly proxyInstallationApiService: ProxyInstallationApiService) {
    this.loadGuide();
  }

  openTerminal(): void {
    // Only reset session state when actually starting a new terminal. If it is already open (and
    // possibly already connected/authenticated), keep its state so we don't lock ourselves out.
    if (!this.terminalVisible) {
      this.terminalBlocked = false;
      this.terminalReady = false;
      this.sudoAuthenticated = false;
      this.sudoAuthPending = false;
    }
    this.terminalVisible = true;
  }

  onTerminalReady(ready: boolean): void {
    this.terminalReady = ready;
    if (!ready) {
      this.sudoAuthenticated = false;
      this.sudoAuthPending = false;
    }
  }

  onTerminalBlocked(blocked: boolean): void {
    this.terminalBlocked = blocked;
    if (blocked) {
      this.sudoAuthenticated = false;
      this.sudoAuthPending = false;
    }
  }

  /**
   * Trigger up-front sudo authentication in the terminal. The editor stays locked until the sudo
   * password is accepted (detected via a success marker in the terminal output).
   */
  requestSudoAuth(): void {
    if (!this.hasValidUser || !this.terminalReady || this.terminalBlocked || this.sudoAuthenticated) {
      return;
    }
    this.sudoAuthPending = true;
    this.editorStatus = 'Answer the sudo prompt in the terminal…';
    this.terminal?.armSentinel(ProxyInstallationComponent.SUDO_SENTINEL);
    this.terminal?.runText(ProxyInstallationComponent.SUDO_AUTH_COMMAND);
  }

  onSudoAuthenticated(): void {
    this.sudoAuthenticated = true;
    this.sudoAuthPending = false;
    this.editorStatus = 'sudo authenticated — you can now edit and write the file.';
  }

  /** True when a syntactically valid, non-blank run-as user has been entered. */
  get hasValidUser(): boolean {
    return ProxyInstallationComponent.USERNAME_PATTERN.test(this.terminalUser.trim());
  }

  /**
   * Writing the site file requires a valid connected user AND an up-front sudo authentication in the
   * terminal (so the editor is usable only after sudo has been unlocked).
   */
  get canWriteConfig(): boolean {
    return (
      this.hasValidUser &&
      this.terminalReady &&
      !this.terminalBlocked &&
      !this.editorLoading &&
      this.sudoAuthenticated
    );
  }

  closeTerminal(): void {
    this.terminalVisible = false;
  }

  /** Run a guide command inside the live interactive shell. */
  useCommand(command: string): void {
    this.openTerminal();
    this.sentCommands.add(command);
    // Wait a tick so the terminal component exists on first open.
    setTimeout(() => this.terminal?.runText(command), 0);
  }

  /** Scope the terminal's "Required commands" list to a single procedure step and open it. */
  selectCommandGroup(group: 'all' | 'install' | 'proxy'): void {
    this.activeCommandGroup = group;
    this.openTerminal();
    // Configuration mode exposes an "Authenticate sudo" button; we do NOT auto-send the auth command
    // because it would race with the shell's own startup (e.g. `sudo -u root -i`'s password prompt).
  }

  /** Configuration mode = the reverse-proxy step is selected. */
  get isConfigMode(): boolean {
    return this.activeCommandGroup === 'proxy';
  }

  /** Launch the editor. Only usable once sudo has been authenticated (button is gated on this). */
  openConfigEditor(): void {
    if (!this.sudoAuthenticated) {
      return;
    }
    this.editorVisible = true;
    this.loadSiteFile();
  }

  closeConfigEditor(): void {
    this.editorVisible = false;
  }

  /** Reload the site file from the server (existing content, or a fresh template). */
  resetConfigEditor(): void {
    this.loadSiteFile();
  }

  /** Fetch the current site file: existing content if present, otherwise a prefilled template. */
  private loadSiteFile(): void {
    this.editorLoading = true;
    this.editorStatus = 'Loading…';
    this.proxyInstallationApiService.getSiteFile().subscribe({
      next: (file) => {
        this.editorContent = file.content;
        this.editorFileExists = file.exists;
        this.editorLoading = false;
        this.editorStatus = file.exists
          ? 'Loaded existing file from the server.'
          : 'No file yet — template prefilled to proxy this application.';
      },
      error: () => {
        this.editorContent = ProxyInstallationComponent.DEFAULT_SITE_CONFIG;
        this.editorFileExists = false;
        this.editorLoading = false;
        this.editorStatus = 'Could not read the server file — showing a default template.';
      }
    });
  }

  /**
   * Write the editor content to the site file through the live shell. Uses a quoted heredoc so nginx
   * variables ($host, ...) are preserved, and `sudo -k tee` so the operator must (re)authenticate
   * with their sudo password every time the root-owned file is written — the prompt appears in the
   * terminal. Only permitted once a valid user is connected (see {@link canWriteConfig}).
   */
  saveConfigFile(): void {
    if (!this.canWriteConfig) {
      this.editorStatus = !this.hasValidUser
        ? 'Set a valid user before writing the file.'
        : 'Authenticate sudo in the terminal to unlock writing.';
      return;
    }
    const marker = 'GS_NGINX_EOF';
    const body = this.editorContent.replace(/\r\n/g, '\n').replace(/\n$/, '');
    const dir = this.siteFilePath.substring(0, this.siteFilePath.lastIndexOf('/'));
    // Ensure the target directory exists (it may not until nginx is installed), then write.
    // sudo is already authenticated up front, so both commands reuse the cached credential.
    const command =
      `sudo mkdir -p ${dir} && sudo tee ${this.siteFilePath} > /dev/null <<'${marker}'\n${body}\n${marker}`;
    // Route the write to the original web terminal.
    this.terminal?.runText(command);
    this.editorStatus = 'Written to ' + this.siteFilePath + ' via the terminal.';
  }

  /**
   * Enable the site (symlink into sites-enabled), validate the config, and reload nginx — all in the
   * sudo-authenticated terminal. Enabled only once sudo has been authenticated.
   */
  enableSiteAndReload(): void {
    if (!this.hasValidUser || !this.terminalReady || this.terminalBlocked || !this.sudoAuthenticated) {
      return;
    }
    const linkName = this.siteFilePath.substring(this.siteFilePath.lastIndexOf('/') + 1);
    const enabledLink = `/etc/nginx/sites-enabled/${linkName}`;
    // Symlink → test config → reload; each step gated on the previous succeeding.
    const command =
      `sudo ln -sf ${this.siteFilePath} ${enabledLink} && sudo nginx -t && sudo systemctl reload nginx`;
    this.terminal?.runText(command);
  }

  /** Commands shown in the terminal, filtered by the active step. */
  get displayedCommands(): string[] {
    const commands = this.guide?.commands ?? [];
    if (this.activeCommandGroup === 'install') {
      return commands.filter((command) => !this.isProxyCommand(command));
    }
    if (this.activeCommandGroup === 'proxy') {
      return commands.filter((command) => this.isProxyCommand(command));
    }
    return commands;
  }

  commandGroupLabel(): string {
    if (this.activeCommandGroup === 'install') {
      return 'Install NGINX';
    }
    if (this.activeCommandGroup === 'proxy') {
      return 'Configure reverse proxy';
    }
    return 'All steps';
  }

  private isProxyCommand(command: string): boolean {
    return ProxyInstallationComponent.PROXY_COMMAND_MARKERS.some((marker) =>
      command.includes(marker)
    );
  }

  clearTerminal(): void {
    this.terminal?.clear();
  }

  commandStateLabel(command: string): string {
    return this.sentCommands.has(command) ? 'sent' : 'pending';
  }

  isCommandSelectable(): boolean {
    return true;
  }

  onTerminalHeaderMouseDown(event: MouseEvent): void {
    this.beginDrag(event, this.terminalLeft, this.terminalTop, (left, top) => {
      this.terminalLeft = left;
      this.terminalTop = top;
    });
  }

  onEditorHeaderMouseDown(event: MouseEvent): void {
    this.beginDrag(event, this.editorLeft, this.editorTop, (left, top) => {
      this.editorLeft = left;
      this.editorTop = top;
    });
  }

  private beginDrag(
    event: MouseEvent,
    startLeft: number,
    startTop: number,
    apply: (left: number, top: number) => void
  ): void {
    const offsetX = event.clientX - startLeft;
    const offsetY = event.clientY - startTop;

    const moveHandler = (moveEvent: MouseEvent) => {
      apply(Math.max(0, moveEvent.clientX - offsetX), Math.max(0, moveEvent.clientY - offsetY));
    };

    const upHandler = () => {
      document.removeEventListener('mousemove', moveHandler);
      document.removeEventListener('mouseup', upHandler);
    };

    document.addEventListener('mousemove', moveHandler);
    document.addEventListener('mouseup', upHandler);
  }

  private loadGuide(): void {
    this.proxyInstallationApiService.getGuide().subscribe({
      next: (guide) => {
        this.guide = guide;
        this.guideError = '';
        this.sentCommands.clear();
      },
      error: () => {
        this.guide = null;
        this.guideError = 'Failed to load installation guide.';
      }
    });
  }
}
