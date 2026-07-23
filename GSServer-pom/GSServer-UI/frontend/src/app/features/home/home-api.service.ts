import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HardeningResponse {
  status: string;
  message: string;
}

interface HardeningRequest {
  tenantId: string;
  requestedBy: string;
  profile: string;
}

@Injectable({
  providedIn: 'root'
})
export class HomeApiService {
  private readonly defaultRequest: HardeningRequest = {
    tenantId: 'tenant-a',
    requestedBy: 'ui-operator',
    profile: 'baseline'
  };

  constructor(private readonly http: HttpClient) {}

  triggerHardening(): Observable<HardeningResponse> {
    return this.http.post<HardeningResponse>('/api/v1/hardening', this.defaultRequest);
  }
}
