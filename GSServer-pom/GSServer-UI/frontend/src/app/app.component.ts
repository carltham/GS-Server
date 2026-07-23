import { Component } from '@angular/core';

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

  constructor(
    private readonly authService: AuthService,
    private readonly themeService: ThemeService
  ) {
    this.themeService.initialize();
    this.authService.initialize().subscribe();
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated;
  }

  get username(): string {
    return this.authService.username;
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
