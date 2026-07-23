import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const authHeader = this.authService.getAuthorizationHeader();

    const authRequest =
      authHeader && request.url.startsWith('/api/')
        ? request.clone({ setHeaders: { Authorization: authHeader } })
        : request;

    return next.handle(authRequest).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.authService.logout();
        }
        return throwError(() => error);
      })
    );
  }
}
