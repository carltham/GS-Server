import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagedUserSummary {
  username: string;
  authorities: string[];
  enabled: boolean;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  authorities: string[];
  enabled: boolean;
}

export interface UpdateUserRequest {
  authorities: string[];
  enabled: boolean;
}

/** Client for the admin/superadmin user-management API. */
@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private readonly base = '/api/v1/admin/users';

  constructor(private readonly http: HttpClient) {}

  list(): Observable<ManagedUserSummary[]> {
    return this.http.get<ManagedUserSummary[]>(this.base);
  }

  create(request: CreateUserRequest): Observable<ManagedUserSummary> {
    return this.http.post<ManagedUserSummary>(this.base, request);
  }

  update(username: string, request: UpdateUserRequest): Observable<ManagedUserSummary> {
    return this.http.put<ManagedUserSummary>(`${this.base}/${encodeURIComponent(username)}`, request);
  }

  resetPassword(username: string, password: string): Observable<void> {
    return this.http.put<void>(`${this.base}/${encodeURIComponent(username)}/password`, { password });
  }

  delete(username: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${encodeURIComponent(username)}`);
  }
}
