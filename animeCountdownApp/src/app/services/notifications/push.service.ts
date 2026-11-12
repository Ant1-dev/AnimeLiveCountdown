import { effect, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../auth/auth.service';
import { NotificationService } from './notification.service';

export type PushState = 'unsupported' | 'unavailable' | 'denied' | 'off' | 'on';

/**
 * Registers the service worker (/sw.js, which also makes the site installable) and manages this
 * browser's push subscription. The subscription is re-sent to the API whenever a signed-in user
 * loads the app, so it stays attached to the current account.
 */
@Injectable({ providedIn: 'root' })
export class PushService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private notifications = inject(NotificationService);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  /** Whether this browser can do Web Push at all (iOS Safari only can once installed). */
  readonly supported =
    this.isBrowser && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  readonly state = signal<PushState>('unsupported');

  private registration: Promise<ServiceWorkerRegistration | null> = Promise.resolve(null);
  private vapidKey: string | null = null;

  constructor() {
    if (!this.isBrowser || !('serviceWorker' in navigator)) return;

    this.registration = navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch((error) => {
      console.warn('Service worker registration failed', error);
      return null;
    });

    effect(() => {
      // Re-run when the user signs in or out
      if (this.auth.user()) this.syncState(true);
    });
  }

  /** Re-reads permission and subscription state (e.g. when a settings view opens). */
  refreshState(): Promise<void> {
    return this.syncState(false);
  }

  /** Asks for permission if needed, subscribes this browser and registers it with the API. */
  async enable(): Promise<PushState> {
    if (!this.supported) return 'unsupported';
    const config = await firstValueFrom(this.notifications.config());
    if (!config.pushEnabled || !config.vapidPublicKey) {
      this.state.set('unavailable');
      return 'unavailable';
    }
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      this.state.set(permission === 'denied' ? 'denied' : 'off');
      return this.state();
    }
    const registration = await this.ready();
    if (!registration) return 'unsupported';

    let subscription = await registration.pushManager.getSubscription();
    if (subscription && !this.matchesKey(subscription, config.vapidPublicKey)) {
      await subscription.unsubscribe();
      subscription = null;
    }
    subscription ??= await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: base64UrlToBytes(config.vapidPublicKey),
    });
    await firstValueFrom(this.http.post(environment.apiUrl + 'auth/push/subscribe', subscription.toJSON()));
    this.state.set('on');
    return 'on';
  }

  async disable(): Promise<void> {
    const registration = await this.ready();
    const subscription = await registration?.pushManager.getSubscription();
    if (subscription) {
      const endpoint = subscription.endpoint;
      await subscription.unsubscribe();
      if (this.auth.user()) {
        await firstValueFrom(this.http.post(environment.apiUrl + 'auth/push/unsubscribe', { endpoint })).catch(() => {});
      }
    }
    this.state.set(this.supported ? 'off' : 'unsupported');
  }

  sendTest(): Promise<{ sent: number }> {
    return firstValueFrom(this.http.post<{ sent: number }>(environment.apiUrl + 'auth/push/test', null));
  }

  private async syncState(resendToServer: boolean): Promise<void> {
    if (!this.supported) {
      this.state.set('unsupported');
      return;
    }
    const config = await firstValueFrom(this.notifications.config());
    this.vapidKey = config.vapidPublicKey;
    if (!config.pushEnabled || !config.vapidPublicKey) {
      this.state.set('unavailable');
      return;
    }
    if (Notification.permission === 'denied') {
      this.state.set('denied');
      return;
    }
    const registration = await this.registration;
    const subscription = await registration?.pushManager.getSubscription();
    const active = !!subscription && this.matchesKey(subscription, config.vapidPublicKey) && Notification.permission === 'granted';
    this.state.set(active ? 'on' : 'off');
    if (active && resendToServer && this.auth.user()) {
      this.http.post(environment.apiUrl + 'auth/push/subscribe', subscription!.toJSON()).subscribe({ error: () => {} });
    }
  }

  private async ready(): Promise<ServiceWorkerRegistration | null> {
    const registration = await this.registration;
    return registration ? navigator.serviceWorker.ready : null;
  }

  private matchesKey(subscription: PushSubscription, vapidKey: string): boolean {
    const current = subscription.options?.applicationServerKey;
    if (!current) return true;
    const expected = base64UrlToBytes(vapidKey);
    const actual = new Uint8Array(current);
    return actual.length === expected.length && actual.every((byte, i) => byte === expected[i]);
  }
}

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - (value.length % 4)) % 4);
  const raw = atob(padded);
  const bytes = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return bytes;
}
