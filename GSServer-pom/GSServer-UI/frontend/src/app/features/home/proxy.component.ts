import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { ProxyApiService, ProxyOperationState, ProxyRuntimeStatus } from './proxy-api.service';

@Component({
  selector: 'gs-proxy',
  templateUrl: './proxy.component.html'
})
export class ProxyComponent {
  enabled = true;
  upstreamHost = 'api.internal.local';
  upstreamPort = 8443;
  tlsEnabled = true;
  isSubmitting = false;
  statusMessage = 'Ready to apply proxy configuration.';
  runtimeMessage = 'Detecting proxy server runtime...';
  runtimeStatus: ProxyRuntimeStatus | null = null;
  latestState: ProxyOperationState | null = null;
  latestStateMessage = 'Loading latest proxy configuration...';

  get noServerDetected(): boolean {
    return this.runtimeStatus != null && !this.runtimeStatus.nginxRunning && !this.runtimeStatus.apacheRunning;
  }

  constructor(private readonly proxyApiService: ProxyApiService) {
    this.loadRuntimeStatus();
  }

  apply(): void {
    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.statusMessage = 'Applying proxy configuration...';

    this.proxyApiService
      .applyProxyConfig({
        tenantId: 'tenant-a',
        requestedBy: 'thor',
        enabled: this.enabled,
        upstreamHost: this.upstreamHost,
        upstreamPort: Number(this.upstreamPort),
        tlsEnabled: this.tlsEnabled
      })
      .subscribe({
        next: (response) => {
          this.statusMessage = response.message;
          this.loadLatestState();
          this.isSubmitting = false;
        },
        error: (error: unknown) => {
          this.statusMessage = this.resolveErrorMessage(error);
          this.isSubmitting = false;
        }
      });
  }

  private resolveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const errorMessage = (error.error as { message?: unknown } | null)?.message;
      if (typeof errorMessage === 'string' && errorMessage.trim().length > 0) {
        return errorMessage;
      }
    }

    return 'Proxy configuration failed.';
  }

  private loadRuntimeStatus(): void {
    this.proxyApiService.getRuntimeStatus().subscribe({
      next: (status) => {
        this.runtimeStatus = status;
        if (status.nginxRunning) {
          this.runtimeMessage = 'Runtime detection: NGINX is running.';
          this.loadLatestState();
          return;
        }

        if (status.apacheRunning) {
          this.runtimeMessage = 'Runtime detection: Apache is running.';
          this.loadLatestState();
          return;
        }

        this.runtimeMessage = 'Runtime detection: no NGINX or Apache process detected.';
        this.latestState = null;
        this.latestStateMessage = 'Proxy configuration is unavailable until a proxy server is installed.';
      },
      error: () => {
        this.runtimeStatus = null;
        this.runtimeMessage = 'Runtime detection failed.';
        this.latestState = null;
        this.latestStateMessage = 'Latest proxy configuration unavailable due to runtime detection error.';
      }
    });
  }

  private loadLatestState(): void {
    this.proxyApiService.getLatestProxyState().subscribe({
      next: (state) => {
        this.latestState = state;
        this.latestStateMessage = 'Latest proxy configuration loaded.';
      },
      error: (error: unknown) => {
        this.latestState = null;
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.latestStateMessage = 'No proxy configuration has been applied yet.';
          return;
        }

        this.latestStateMessage = 'Failed to load latest proxy configuration.';
      }
    });
  }
}
