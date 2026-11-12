import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, ReplaySubject, finalize, map, shareReplay, take, tap } from 'rxjs';
import { User } from '../models/user.model';
import { environment } from '../../environments/environment';

/** Set while the browser has a session, so pages can skip "checking your session" placeholders for
 *  visitors who were never signed in. Not a credential: just a yes/no hint. */
export const SESSION_HINT_KEY = 'alc.hadSession';

// Keys the old version of the app kept tokens under; removed on startup.
const LEGACY_STORAGE_KEYS = ['accessToken', 'refreshToken'];

/**
 * Session handling.
 *
 * The refresh token lives in an HttpOnly cookie set by the API, so scripts on this page can never
 * read it. The access token is kept in memory only: on each page load the app calls
 * /auth/refresh, and the cookie is exchanged for a fresh access token.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly authUrl = environment.apiUrl + 'auth/';

  private storedAccessToken = signal<string | null>(null);
  readonly accessToken = this.storedAccessToken.asReadonly();

  private storedUser = signal<User | null>(null);
  readonly user = this.storedUser.asReadonly();
  readonly isAuthenticated = computed(() => this.storedUser() !== null);

  // Emits once the session restore on page load has finished (signed in or not)
  private readonly sessionChecked = new ReplaySubject<void>(1);

  // The refresh request in flight, shared so concurrent 401s trigger a single refresh
  private refreshInFlight: Observable<string> | null = null;

  constructor() {
    if (typeof window === 'undefined') {
      this.sessionChecked.next();
      return;
    }

    this.removeLegacyTokens();
    // Deferred so the request runs after this constructor returns: the HTTP interceptor injects
    // AuthService, and calling it during construction fails with a circular-DI error (which the
    // error handler below would swallow, leaving every visitor signed out on page load).
    queueMicrotask(() =>
      this.refresh().subscribe({
        error: () => this.sessionChecked.next(),
        complete: () => this.sessionChecked.next(),
      })
    );
  }

  /** Resolves once the app knows whether the visitor is signed in. */
  whenSessionChecked(): Observable<void> {
    return this.sessionChecked.pipe(take(1));
  }

  /** Exchanges the refresh token cookie for a new access token. Errors if there is no valid session. */
  refresh(): Observable<string> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http
        .post<{ accessToken: string } | null>(this.authUrl + 'refresh', null, { withCredentials: true })
        .pipe(
          // 204 (no body) means "no session": signed-out visitors, not an error worth logging
          map((response) => {
            if (!response?.accessToken) throw new Error('No session');
            return response.accessToken;
          }),
          tap({
            next: (token) => this.setAccessToken(token),
            error: () => this.clearSession(),
          }),
          finalize(() => (this.refreshInFlight = null)),
          shareReplay(1)
        );
    }
    return this.refreshInFlight;
  }

  isTokenExpired(token: string): boolean {
    const payload = this.decodePayload(token);
    return !payload || payload.exp < Math.floor(Date.now() / 1000);
  }

  logout(): void {
    this.clearSession();
    // Clears the refresh token cookie; the local session is gone either way
    this.http
      .post(this.authUrl + 'logout', null, { withCredentials: true })
      .subscribe({ error: () => {} });
    this.router.navigate(['/']);
  }

  /** Drops the local session without calling the API (used when a refresh fails). */
  clearSession(): void {
    this.storedAccessToken.set(null);
    this.storedUser.set(null);
    writeSessionHint(false);
  }

  private setAccessToken(token: string): void {
    const payload = this.decodePayload(token);
    if (!payload) {
      this.clearSession();
      return;
    }
    writeSessionHint(true);
    this.storedAccessToken.set(token);
    this.storedUser.set({
      id: payload.id,
      email: payload.sub,
      name: payload.name,
      picture: payload.picture,
    });
  }

  private decodePayload(token: string): any | null {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64));
    } catch {
      return null;
    }
  }

  private removeLegacyTokens(): void {
    try {
      LEGACY_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
    } catch {
      // Storage can be unavailable (private mode, blocked site data); nothing to clean up then
    }
  }
}

/** True if this browser had a session last time (see SESSION_HINT_KEY). */
export function readSessionHint(): boolean {
  try {
    return localStorage.getItem(SESSION_HINT_KEY) === '1';
  } catch {
    return false; // server render, or storage blocked
  }
}

function writeSessionHint(hadSession: boolean): void {
  try {
    if (hadSession) localStorage.setItem(SESSION_HINT_KEY, '1');
    else localStorage.removeItem(SESSION_HINT_KEY);
  } catch {
    // Storage unavailable: the feed just shows its skeleton during the session check
  }
}
