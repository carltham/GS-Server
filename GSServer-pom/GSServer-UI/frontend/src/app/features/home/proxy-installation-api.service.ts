import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProxyInstallGuideResponse {
  operatingSystem: string;
  steps: string[];
  commands: string[];
}

export interface SiteFileResponse {
  path: string;
  exists: boolean;
  content: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProxyInstallationApiService {
  constructor(private readonly http: HttpClient) {}

  getGuide(): Observable<ProxyInstallGuideResponse> {
    return this.http.get<ProxyInstallGuideResponse>('/api/v1/proxy/install/guide');
  }

  getSiteFile(): Observable<SiteFileResponse> {
    return this.http.get<SiteFileResponse>('/api/v1/proxy/install/site-file');
  }
}
