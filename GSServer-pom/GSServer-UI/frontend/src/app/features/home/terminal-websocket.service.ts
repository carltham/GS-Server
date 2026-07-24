import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject, Observable } from 'rxjs';

/**
 * Messages exchanged with the PTY-backed terminal WebSocket.
 * Terminal payloads carry raw bytes as Base64.
 */
export type ClientMessage =
  | { type: 'INPUT'; data: string }
  | { type: 'RESIZE'; cols: number; rows: number };

export type ServerMessage =
  | { type: 'OUTPUT'; data: string }
  | { type: 'EXIT'; exitCode: number }
  | { type: 'ERROR'; message: string };

/**
 * Connects to the interactive terminal WebSocket and streams raw PTY bytes
 * in both directions. One connection == one live shell session.
 */
@Injectable({ providedIn: 'root' })
export class TerminalWebSocketService {
  private webSocket: WebSocket | null = null;
  private readonly outputSubject = new Subject<Uint8Array>();
  private readonly statusSubject = new Subject<'open' | 'closed' | 'error'>();
  private readonly exitSubject = new Subject<number>();
  private readonly errorSubject = new Subject<string>();

  private readonly encoder = new TextEncoder();

  constructor(private readonly http: HttpClient) {}

  /**
   * Open a fresh terminal session, closing any previous socket first.
   * @param runAsUser optional OS user the shell should run as (blank = server user)
   */
  connect(runAsUser = ''): void {
    this.disconnect();

    // Browsers cannot set an Authorization header on a WS handshake. Instead we mint a
    // short-lived, single-use ticket over the authenticated HTTP channel, then hand only
    // that ticket to the WebSocket URL. The AuthInterceptor attaches the Basic header here.
    this.http.post<{ ticket: string }>('/api/v1/terminal/ticket', {}).subscribe({
      next: (response) => this.openSocket(response.ticket, runAsUser),
      error: () => this.statusSubject.next('error')
    });
  }

  private openSocket(ticket: string, runAsUser: string): void {
    // Same-origin WebSocket: goes through whatever serves the page — nginx in production
    // (proxied to the backend) and the Angular dev proxy (ws:true) in development.
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.host; // hostname[:port] of the current origin
    const userParam = runAsUser ? `&user=${encodeURIComponent(runAsUser)}` : '';
    const wsUrl =
      `${protocol}://${host}/api/v1/terminal/ws?ticket=${encodeURIComponent(ticket)}${userParam}`;

    const ws = new WebSocket(wsUrl);
    this.webSocket = ws;

    ws.onopen = () => this.statusSubject.next('open');
    ws.onerror = () => this.statusSubject.next('error');
    ws.onclose = () => this.statusSubject.next('closed');
    ws.onmessage = (event) => this.handleMessage(event.data);
  }

  private handleMessage(raw: string): void {
    let message: ServerMessage;
    try {
      message = JSON.parse(raw) as ServerMessage;
    } catch {
      return;
    }

    switch (message.type) {
      case 'OUTPUT':
        this.outputSubject.next(this.base64ToBytes(message.data));
        break;
      case 'EXIT':
        this.exitSubject.next(message.exitCode);
        break;
      case 'ERROR':
        this.errorSubject.next(message.message);
        break;
    }
  }

  /** Send raw keystroke text to the shell. */
  sendInput(text: string): void {
    this.send({ type: 'INPUT', data: this.bytesToBase64(this.encoder.encode(text)) });
  }

  /** Report a new terminal window size to the PTY. */
  sendResize(cols: number, rows: number): void {
    this.send({ type: 'RESIZE', cols, rows });
  }

  private send(message: ClientMessage): void {
    if (this.webSocket?.readyState === WebSocket.OPEN) {
      this.webSocket.send(JSON.stringify(message));
    }
  }

  onOutput(): Observable<Uint8Array> {
    return this.outputSubject.asObservable();
  }

  onStatus(): Observable<'open' | 'closed' | 'error'> {
    return this.statusSubject.asObservable();
  }

  onExit(): Observable<number> {
    return this.exitSubject.asObservable();
  }

  onError(): Observable<string> {
    return this.errorSubject.asObservable();
  }

  isConnected(): boolean {
    return this.webSocket?.readyState === WebSocket.OPEN;
  }

  disconnect(): void {
    if (this.webSocket) {
      this.webSocket.onclose = null;
      this.webSocket.close();
      this.webSocket = null;
    }
  }

  private bytesToBase64(bytes: Uint8Array): string {
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }

  private base64ToBytes(base64: string): Uint8Array {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }
}
