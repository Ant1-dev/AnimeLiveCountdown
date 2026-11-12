import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/**
 * Install-as-app support. Chromium browsers fire `beforeinstallprompt`, which is kept so the app
 * can offer its own Install button. iOS has no prompt: users add the site from the Share menu, and
 * iOS only delivers web push to sites added to the Home Screen.
 */
@Injectable({ providedIn: 'root' })
export class PwaInstallService {
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private deferredPrompt: BeforeInstallPromptEvent | null = null;

  readonly canInstall = signal(false);
  readonly isStandalone = signal(false);
  readonly isIos = signal(false);

  constructor() {
    if (!this.isBrowser) return;

    this.isStandalone.set(
      window.matchMedia?.('(display-mode: standalone)').matches || (navigator as any).standalone === true
    );
    this.isIos.set(/iphone|ipad|ipod/i.test(navigator.userAgent) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1));

    window.addEventListener('beforeinstallprompt', (event) => {
      event.preventDefault();
      this.deferredPrompt = event as BeforeInstallPromptEvent;
      this.canInstall.set(true);
    });
    window.addEventListener('appinstalled', () => {
      this.deferredPrompt = null;
      this.canInstall.set(false);
      this.isStandalone.set(true);
    });
  }

  /** iOS Safari in a normal tab: show "Add to Home Screen" instructions instead of a button. */
  showIosHint(): boolean {
    return this.isIos() && !this.isStandalone();
  }

  async install(): Promise<boolean> {
    if (!this.deferredPrompt) return false;
    const prompt = this.deferredPrompt;
    this.deferredPrompt = null;
    this.canInstall.set(false);
    await prompt.prompt();
    const choice = await prompt.userChoice;
    return choice.outcome === 'accepted';
  }
}
