import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import {
  ProxyInstallGuideResponse,
  ProxyInstallationApiService,
  TerminalCommandResponse
} from './proxy-installation-api.service';

type CommandExecutionState = 'not-run' | 'running' | 'success' | 'failed';

@Component({
  selector: 'gs-proxy-installation',
  templateUrl: './proxy-installation.component.html'
})
export class ProxyInstallationComponent {
  private static readonly TERMINAL_DEFAULT_MESSAGE =
    'Terminal ready. Select an instruction command or type one manually.';

  guide: ProxyInstallGuideResponse | null = null;
  guideError = '';

  terminalVisible = false;
  terminalBusy = false;
  terminalCommand = '';
  terminalOutput = ProxyInstallationComponent.TERMINAL_DEFAULT_MESSAGE;
  commandStates: Record<string, CommandExecutionState> = {};

  terminalLeft = 120;
  terminalTop = 120;

  private dragging = false;
  private dragOffsetX = 0;
  private dragOffsetY = 0;

  constructor(private readonly proxyInstallationApiService: ProxyInstallationApiService) {
    this.loadGuide();
  }

  openTerminal(): void {
    this.terminalVisible = true;
  }

  closeTerminal(): void {
    this.terminalVisible = false;
  }

  clearTerminalOutput(): void {
    this.terminalOutput = ProxyInstallationComponent.TERMINAL_DEFAULT_MESSAGE;
  }

  useCommand(command: string): void {
    if (!this.isCommandSelectable(command)) {
      return;
    }

    this.terminalCommand = command;
    this.openTerminal();
  }

  isCommandSelectable(command: string): boolean {
    return this.commandStates[command] !== 'success' && this.commandStates[command] !== 'running';
  }

  commandStateLabel(command: string): string {
    const state = this.commandStates[command] ?? 'not-run';
    if (state === 'success') {
      return 'done';
    }

    if (state === 'running') {
      return 'running';
    }

    if (state === 'failed') {
      return 'failed';
    }

    return 'pending';
  }

  startNginxInstallation(): void {
    if (!this.guide || this.guide.commands.length === 0) {
      this.openTerminal();
      return;
    }

    this.openTerminal();
    this.terminalOutput =
      'Terminal ready. Execute the following commands in order to install and start NGINX:';

    for (const command of this.guide.commands) {
      this.appendOutputLine(command);
    }

    this.terminalCommand =
      this.guide.commands.find((command) => command.includes('sudo apt update')) ?? this.guide.commands[0];
  }

  runCurrentCommand(): void {
    const command = this.terminalCommand.trim();
    if (command.length === 0 || this.terminalBusy) {
      return;
    }

    this.terminalBusy = true;
    this.commandStates[command] = 'running';
    this.appendOutputLine('$ ' + command);

    this.proxyInstallationApiService.executeCommand({ command }).subscribe({
      next: (response) => {
        this.appendCommandResponse(response);
        this.commandStates[command] = response.exitCode === 0 ? 'success' : 'failed';
        this.terminalBusy = false;
      },
      error: (error: unknown) => {
        this.appendOutputLine(this.resolveErrorMessage(error));
        this.commandStates[command] = 'failed';
        this.terminalBusy = false;
      }
    });
  }

  onTerminalHeaderMouseDown(event: MouseEvent): void {
    this.dragging = true;
    this.dragOffsetX = event.clientX - this.terminalLeft;
    this.dragOffsetY = event.clientY - this.terminalTop;

    const moveHandler = (moveEvent: MouseEvent) => {
      if (!this.dragging) {
        return;
      }

      this.terminalLeft = Math.max(0, moveEvent.clientX - this.dragOffsetX);
      this.terminalTop = Math.max(0, moveEvent.clientY - this.dragOffsetY);
    };

    const upHandler = () => {
      this.dragging = false;
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
        this.commandStates = {};
        for (const command of guide.commands) {
          this.commandStates[command] = 'not-run';
        }

        if (guide.commands.length > 0) {
          this.terminalCommand = guide.commands[0];
        }
      },
      error: () => {
        this.guide = null;
        this.guideError = 'Failed to load installation guide.';
      }
    });
  }

  private appendCommandResponse(response: TerminalCommandResponse): void {
    this.appendOutputLine('exit=' + response.exitCode + ' duration=' + response.durationMs + 'ms');
    if (response.stdout.trim().length > 0) {
      this.appendOutputLine(response.stdout.trimEnd());
    }
    if (response.stderr.trim().length > 0) {
      this.appendOutputLine(response.stderr.trimEnd());
    }
  }

  private appendOutputLine(text: string): void {
    this.terminalOutput = this.terminalOutput + '\n' + text;
  }

  private resolveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const message = (error.error as { message?: unknown } | null)?.message;
      if (typeof message === 'string' && message.trim().length > 0) {
        return message;
      }
    }
    return 'Command execution failed.';
  }
}
