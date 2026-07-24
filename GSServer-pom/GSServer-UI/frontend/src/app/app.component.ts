import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { AuthService } from './core/auth.service';
import { ThemeService } from './core/theme.service';

@Component({
  selector: 'gs-root',
  templateUrl: './app.component.html'
})
export class AppComponent {
  usernameInput = '';
  passwordInput = '';
  loginError = '';
  isSubmitting = false;
  thorLoginEnabled = false;

  constructor(
    private readonly authService: AuthService,
    private readonly themeService: ThemeService,
    private readonly http: HttpClient
  ) {
    this.themeService.initialize();

    // Passwordless Thor login is only offered when the server explicitly enables it.
    this.http
      .get<{ thorLoginEnabled: boolean }>('/api/v1/auth/config')
      .subscribe({
        next: (config) => (this.thorLoginEnabled = config.thorLoginEnabled),
        error: () => (this.thorLoginEnabled = false)
      });

    // A `?logout=true` link logs the user out and redirects to the app root.
    const params = new URLSearchParams(window.location.search);
    if (params.get('logout') === 'true') {
      this.authService.logout();
      window.history.replaceState({}, '', '/');
    }

    this.authService.initialize().subscribe();
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated;
  }

  get username(): string {
    return this.authService.username;
  }

  get canManageUsers(): boolean {
    const authorities = this.authService.authorities;
    return authorities.includes('GROUP_HARDENING_ADMINS') || authorities.includes('GROUP_SUPERUSER');
  }

  get darkThemeEnabled(): boolean {
    return this.themeService.isDarkTheme();
  }

  login(): void {
    if (this.isSubmitting) {
      return;
    }

    this.loginError = '';
    this.isSubmitting = true;

    this.authService.login(this.usernameInput.trim(), this.passwordInput).subscribe((success) => {
      this.isSubmitting = false;
      if (!success) {
        this.loginError = 'Login failed. Check username, password, and access policy.';
        return;
      }

      this.passwordInput = '';
    });
  }

  loginAsThor(): void {
    if (this.isSubmitting) {
      return;
    }

    this.usernameInput = 'thor';
    this.passwordInput = '';
    this.login();
  }

  logout(): void {
    this.authService.logout();
    this.usernameInput = '';
    this.passwordInput = '';
    this.loginError = '';
  }

  onThemeToggle(enabled: boolean): void {
    this.themeService.setDarkTheme(enabled);
  }
}
