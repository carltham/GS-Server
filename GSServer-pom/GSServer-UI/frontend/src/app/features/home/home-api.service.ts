import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HardeningResponse {
  status: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class HomeApiService {
  constructor(private readonly http: HttpClient) {}

  triggerHardening(): Observable<HardeningResponse> {
    return this.http.post<HardeningResponse>('/api/v1/hardening', {});
  }
}
