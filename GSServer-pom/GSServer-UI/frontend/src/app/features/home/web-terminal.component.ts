import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';

import { TerminalWebSocketService } from './terminal-websocket.service';

/**
 * A real interactive terminal rendered with xterm.js and backed by a server-side
 * PTY shell over a WebSocket. Keystrokes are streamed to the shell and raw PTY
 * output (colours, prompts, full-screen programs, sudo password prompts) is
 * rendered faithfully.
 */
@Component({
  selector: 'gs-web-terminal',
  template: '<div #host class="gs-web-terminal-host"></div>',
  styles: [
    `
      :host {
        display: block;
        width: 100%;
        height: 100%;
        background: #1e1e1e;
        overscroll-behavior: contain;
      }
      .gs-web-terminal-host {
        width: 100%;
        height: 100%;
        padding: 6px 8px;
        box-sizing: border-box;
      }
    `
  ]
})
export class WebTerminalComponent implements AfterViewInit, OnDestroy {
  @ViewChild('host', { static: true }) hostRef!: ElementRef<HTMLDivElement>;

  /** OS user the shell runs as; blank means the server user. Read once when the socket opens. */
  @Input() runAsUser = '';

  /** Emits true when the session is rejected/blocked and the terminal can only be closed. */
  @Output() blockedChange = new EventEmitter<boolean>();

  /** Emits true once the shell is connected as a valid user, false when it stops being usable. */
  @Output() readyChange = new EventEmitter<boolean>();

  /** Emits once when the armed sentinel string appears in the terminal output. */
  @Output() sentinelSeen = new EventEmitter<void>();

  blocked = false;
  private everOpened = false;
  private readonly textDecoder = new TextDecoder();
  private sentinel: string | null = null;
  private sentinelBuffer = '';

  private terminal!: Terminal;
  private fitAddon!: FitAddon;
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();
  private readonly pendingInput: string[] = [];

  constructor(private readonly webSocketService: TerminalWebSocketService) {}

  ngAfterViewInit(): void {
    this.terminal = new Terminal({
      cursorBlink: true,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      fontSize: 13,
      scrollback: 5000,
      theme: { background: '#1e1e1e', foreground: '#e0e0e0' }
    });

    this.fitAddon = new FitAddon();
    this.terminal.loadAddon(this.fitAddon);
    this.terminal.open(this.hostRef.nativeElement);
    this.fit();

    // Keystrokes -> shell
    this.terminal.onData((data) => this.webSocketService.sendInput(data));

    // Shell output -> terminal (and sentinel detection)
    this.webSocketService
      .onOutput()
      .pipe(takeUntil(this.destroy$))
      .subscribe((bytes) => {
        this.terminal.write(bytes);
        this.scanSentinel(bytes);
      });

    this.webSocketService
      .onStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe((status) => this.handleStatus(status));

    this.webSocketService
      .onExit()
      .pipe(takeUntil(this.destroy$))
      .subscribe((code) => this.terminal.write(`\r\n\x1b[90m[shell exited with code ${code}]\x1b[0m\r\n`));

    this.webSocketService
      .onError()
      .pipe(takeUntil(this.destroy$))
      .subscribe((message) => this.block(message));

    // Keep the PTY size in sync with the rendered element.
    this.resizeObserver = new ResizeObserver(() => this.fit());
    this.resizeObserver.observe(this.hostRef.nativeElement);

    this.webSocketService.connect(this.runAsUser);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.resizeObserver?.disconnect();
    this.webSocketService.disconnect();
    this.terminal?.dispose();
  }

  /** Type text into the shell as if the user had entered it (used by command shortcuts). */
  runText(text: string, execute = true): void {
    if (this.blocked) {
      return;
    }
    const payload = execute ? `${text}\n` : text;
    if (this.webSocketService.isConnected()) {
      this.webSocketService.sendInput(payload);
    } else {
      this.pendingInput.push(payload);
    }
    this.terminal?.focus();
  }

  focus(): void {
    this.terminal?.focus();
  }

  /**
   * Watch the terminal output for a one-shot marker string; {@link sentinelSeen} fires when it is
   * next produced. Re-arming resets the match.
   */
  armSentinel(sentinel: string): void {
    this.sentinel = sentinel;
    this.sentinelBuffer = '';
  }

  private scanSentinel(bytes: Uint8Array): void {
    if (!this.sentinel) {
      return;
    }
    // Keep a bounded rolling tail so a marker split across chunks is still matched.
    this.sentinelBuffer = (this.sentinelBuffer + this.textDecoder.decode(bytes, { stream: true }))
      .slice(-512);
    if (this.sentinelBuffer.includes(this.sentinel)) {
      this.sentinel = null;
      this.sentinelBuffer = '';
      this.sentinelSeen.emit();
    }
  }

  /** Clear the terminal viewport, keeping the current prompt line. */
  clear(): void {
    this.terminal?.clear();
    this.terminal?.focus();
  }

  private handleStatus(status: 'open' | 'closed' | 'error'): void {
    if (status === 'open') {
      this.everOpened = true;
      this.readyChange.emit(true);
      // Push the current size to the freshly-started PTY.
      this.fit();
      // Flush any command shortcuts queued before the socket opened.
      while (this.pendingInput.length > 0) {
        this.webSocketService.sendInput(this.pendingInput.shift()!);
      }
      this.terminal.focus();
    } else if (status === 'error' && !this.everOpened) {
      this.block('Could not start the terminal session.');
    } else if (status === 'closed') {
      this.readyChange.emit(false);
      if (!this.everOpened) {
        this.block('The session was rejected before it started.');
      }
    }
  }

  /** Put the terminal into a terminal (pun intended) blocked state: no input, close-only. */
  private block(reason: string): void {
    if (this.blocked) {
      return;
    }
    this.blocked = true;
    this.pendingInput.length = 0;
    this.blockedChange.emit(true);
    this.readyChange.emit(false);
    this.terminal?.write(
      `\r\n\x1b[41;97m BLOCKED \x1b[0m \x1b[31m${reason}\x1b[0m\r\n` +
        `\x1b[90mClose the terminal and choose a different user.\x1b[0m\r\n`
    );
  }

  private fit(): void {
    if (!this.fitAddon) {
      return;
    }
    try {
      this.fitAddon.fit();
      if (this.webSocketService.isConnected()) {
        this.webSocketService.sendResize(this.terminal.cols, this.terminal.rows);
      }
    } catch {
      // element not measurable yet
    }
  }
}
