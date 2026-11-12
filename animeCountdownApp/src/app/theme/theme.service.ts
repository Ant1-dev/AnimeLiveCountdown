import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

/** localStorage key; index.html reads the same key before first paint. */
export const THEME_STORAGE_KEY = 'alc-theme';

/**
 * Dark/light theme. The theme is the `data-theme` attribute on <html>; styles.scss swaps its
 * color tokens on it. With no saved choice we follow prefers-color-scheme (and keep following
 * it if the OS setting changes); once the user picks a theme it is saved in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private document = inject(DOCUMENT);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  readonly theme = signal<Theme>('dark');

  constructor() {
    if (!this.isBrowser) return;
    const saved = this.readSaved();
    this.apply(saved ?? this.systemTheme());

    try {
      window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', (e) => {
        if (!this.readSaved()) this.apply(e.matches ? 'light' : 'dark');
      });
    } catch {
      // Old browsers without matchMedia listeners just keep the initial theme.
    }
  }

  toggle(): void {
    this.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this.apply(theme);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Storage blocked (private mode, disabled cookies): the choice lasts for this page only.
    }
  }

  private apply(theme: Theme): void {
    this.theme.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);
    this.document.querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', theme === 'dark' ? '#101116' : '#f4f6fa');
  }

  private readSaved(): Theme | null {
    try {
      const value = localStorage.getItem(THEME_STORAGE_KEY);
      return value === 'light' || value === 'dark' ? value : null;
    } catch {
      return null;
    }
  }

  private systemTheme(): Theme {
    try {
      return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  }
}
