import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProxyRequest {
  tenantId: string;
  requestedBy: string;
  enabled: boolean;
  upstreamHost: string;
  upstreamPort: number;
  tlsEnabled: boolean;
}

export interface ProxyResponse {
  status: string;
  message: string;
}

export interface ProxyRuntimeStatus {
  nginxRunning: boolean;
  apacheRunning: boolean;
  detectedServer: string;
}

export interface ProxyOperationState {
  operationId: string;
  occurredAtUtc: string;
  status: string;
  tenantId: string;
  requestedBy: string;
  enabled: boolean;
  upstreamHost: string;
  upstreamPort: number;
  tlsEnabled: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProxyApiService {
  constructor(private readonly http: HttpClient) {}

  applyProxyConfig(request: ProxyRequest): Observable<ProxyResponse> {
    return this.http.post<ProxyResponse>('/api/v1/proxy', request);
  }

  getRuntimeStatus(): Observable<ProxyRuntimeStatus> {
    return this.http.get<ProxyRuntimeStatus>('/api/v1/proxy/runtime');
  }

  getLatestProxyState(): Observable<ProxyOperationState> {
    return this.http.get<ProxyOperationState>('/api/v1/proxy/latest');
  }
}
