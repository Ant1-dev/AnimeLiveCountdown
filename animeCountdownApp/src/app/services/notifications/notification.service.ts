import { DestroyRef, effect, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../auth/auth.service';
import { AppNotification, NotificationConfig, NotificationList } from '../../models/notification.model';

const POLL_INTERVAL_MS = 60_000;
const DISABLED_CONFIG: NotificationConfig = { pushEnabled: false, vapidPublicKey: null, discordEnabled: false };

/**
 * The notification bell's data: recent notifications and the unread count for the signed-in user.
 * Polls while the tab is visible and refreshes immediately when the service worker receives a push.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly items = signal<AppNotification[]>([]);
  readonly unread = signal(0);
  readonly loaded = signal(false);

  private config$?: Observable<NotificationConfig>;
  private pollTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    if (!this.isBrowser) return;

    effect(() => {
      if (this.auth.user()) {
        this.refresh();
        this.startPolling();
      } else {
        this.stopPolling();
        this.items.set([]);
        this.unread.set(0);
        this.loaded.set(false);
      }
    });

    navigator.serviceWorker?.addEventListener('message', (event) => {
      if (event.data?.type === 'notification-received' && this.auth.user()) this.refresh();
    });
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && this.auth.user()) this.refresh();
    });
    inject(DestroyRef).onDestroy(() => this.stopPolling());
  }

  /** Which channels the server has configured; cached for the session. */
  config(): Observable<NotificationConfig> {
    this.config$ ??= this.http
      .get<NotificationConfig>(environment.apiUrl + 'notifications/config')
      .pipe(
        catchError(() => of(DISABLED_CONFIG)),
        shareReplay(1)
      );
    return this.config$;
  }

  refresh(): void {
    this.http.get<NotificationList>(environment.apiUrl + 'auth/notifications').subscribe({
      next: (list) => {
        this.items.set(list.items);
        this.unread.set(list.unread);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }

  markRead(notification: AppNotification): void {
    if (notification.read) return;
    this.items.update((items) => items.map((n) => (n.id === notification.id ? { ...n, read: true } : n)));
    this.unread.update((count) => Math.max(0, count - 1));
    this.http.post(environment.apiUrl + `auth/notifications/${notification.id}/read`, null).subscribe({ error: () => {} });
  }

  markAllRead(): void {
    this.items.update((items) => items.map((n) => ({ ...n, read: true })));
    this.unread.set(0);
    this.http.post(environment.apiUrl + 'auth/notifications/read-all', null).subscribe({ error: () => {} });
  }

  private startPolling(): void {
    if (this.pollTimer) return;
    this.pollTimer = setInterval(() => {
      if (document.visibilityState === 'visible' && this.auth.user()) this.refresh();
    }, POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimer) clearInterval(this.pollTimer);
    this.pollTimer = null;
  }
}
