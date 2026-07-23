import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

interface AuthIdentityResponse {
  username: string;
  authorities: string[];
}

interface AuthSessionState {
  token: string;
  username: string;
  authorities: string[];
  authenticatedAtUtc: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly AUTH_STORAGE_KEY = 'gs.auth.session';

  private readonly authenticatedSubject = new BehaviorSubject<boolean>(false);
  private readonly usernameSubject = new BehaviorSubject<string>('');
  private readonly authoritiesSubject = new BehaviorSubject<string[]>([]);

  readonly authenticated$ = this.authenticatedSubject.asObservable();
  readonly username$ = this.usernameSubject.asObservable();
  readonly authorities$ = this.authoritiesSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  initialize(): Observable<boolean> {
    const session = this.readSession();
    if (!session) {
      this.clearState();
      return of(false);
    }

    this.authenticatedSubject.next(true);
    this.usernameSubject.next(session.username);
    this.authoritiesSubject.next(session.authorities);

    return this.fetchIdentity(session.token).pipe(
      tap((identity) => {
        this.persistSession({
          token: session.token,
          username: identity.username,
          authorities: identity.authorities,
          authenticatedAtUtc: session.authenticatedAtUtc
        });
      }),
      map(() => true),
      catchError(() => {
        this.logout();
        return of(false);
      })
    );
  }

  login(username: string, password: string): Observable<boolean> {
    const token = `Basic ${btoa(`${username}:${password}`)}`;

    return this.fetchIdentity(token).pipe(
      tap((identity) => {
        this.persistSession({
          token,
          username: identity.username,
          authorities: identity.authorities,
          authenticatedAtUtc: new Date().toISOString()
        });
      }),
      map(() => true),
      catchError(() => {
        this.clearState();
        return of(false);
      })
    );
  }

  logout(): void {
    this.clearState();
  }

  getAuthorizationHeader(): string | null {
    return this.getToken();
  }

  get isAuthenticated(): boolean {
    return this.authenticatedSubject.value;
  }

  get username(): string {
    return this.usernameSubject.value;
  }

  get authorities(): string[] {
    return this.authoritiesSubject.value;
  }

  private fetchIdentity(token: string): Observable<AuthIdentityResponse> {
    const headers = new HttpHeaders({ Authorization: token });
    return this.http.get<AuthIdentityResponse>('/api/v1/auth/me', { headers });
  }

  private getToken(): string | null {
    return this.readSession()?.token ?? null;
  }

  private readSession(): AuthSessionState | null {
    const raw = sessionStorage.getItem(AuthService.AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<AuthSessionState>;
      if (
        typeof parsed.token !== 'string'
        || typeof parsed.username !== 'string'
        || !Array.isArray(parsed.authorities)
        || typeof parsed.authenticatedAtUtc !== 'string'
      ) {
        return null;
      }

      return {
        token: parsed.token,
        username: parsed.username,
        authorities: parsed.authorities,
        authenticatedAtUtc: parsed.authenticatedAtUtc
      };
    } catch {
      return null;
    }
  }

  private persistSession(state: AuthSessionState): void {
    sessionStorage.setItem(AuthService.AUTH_STORAGE_KEY, JSON.stringify(state));
    this.authenticatedSubject.next(true);
    this.usernameSubject.next(state.username);
    this.authoritiesSubject.next(state.authorities);
  }

  private clearState(): void {
    sessionStorage.removeItem(AuthService.AUTH_STORAGE_KEY);
    this.authenticatedSubject.next(false);
    this.usernameSubject.next('');
    this.authoritiesSubject.next([]);
  }
}
