import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private static readonly THEME_STORAGE_KEY = 'gs.ui.theme';

  constructor(@Inject(DOCUMENT) private readonly document: Document) {}

  initialize(): void {
    const stored = this.readStoredTheme();
    this.applyTheme(stored ?? 'light');
  }

  setDarkTheme(enabled: boolean): void {
    this.applyTheme(enabled ? 'dark' : 'light');
  }

  isDarkTheme(): boolean {
    return this.currentTheme() === 'dark';
  }

  private currentTheme(): ThemeMode {
    const classList = this.document.documentElement.classList;
    return classList.contains('theme-dark') ? 'dark' : 'light';
  }

  private applyTheme(theme: ThemeMode): void {
    const classList = this.document.documentElement.classList;
    classList.remove('theme-light', 'theme-dark');
    classList.add(theme === 'dark' ? 'theme-dark' : 'theme-light');
    sessionStorage.setItem(ThemeService.THEME_STORAGE_KEY, theme);
  }

  private readStoredTheme(): ThemeMode | null {
    const stored = sessionStorage.getItem(ThemeService.THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    return null;
  }
}
