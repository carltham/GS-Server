import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProxyInstallGuideResponse {
  operatingSystem: string;
  steps: string[];
  commands: string[];
}

export interface TerminalCommandRequest {
  command: string;
}

export interface TerminalCommandResponse {
  command: string;
  exitCode: number;
  stdout: string;
  stderr: string;
  durationMs: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProxyInstallationApiService {
  constructor(private readonly http: HttpClient) {}

  getGuide(): Observable<ProxyInstallGuideResponse> {
    return this.http.get<ProxyInstallGuideResponse>('/api/v1/proxy/install/guide');
  }

  executeCommand(request: TerminalCommandRequest): Observable<TerminalCommandResponse> {
    return this.http.post<TerminalCommandResponse>('/api/v1/proxy/install/terminal/execute', request);
  }
}
