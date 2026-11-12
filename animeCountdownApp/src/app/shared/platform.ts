import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { EMPTY, interval, Observable, of, timer } from 'rxjs';

/**
 * Helpers for code that must behave differently during server-side rendering.
 *
 * The server sends a page once the app has no pending work, so a timer that keeps firing would
 * hold every request open until it times out. On the server these helpers fire once (so the page
 * still loads its data) or never.
 */

/** True in the browser, false while rendering on the server. Call in an injection context. */
export function injectIsBrowser(): boolean {
  return isPlatformBrowser(inject(PLATFORM_ID));
}

/** `timer(0, period)` in the browser; a single immediate emission on the server. */
export function pollEvery(period: number, isBrowser: boolean): Observable<number> {
  return isBrowser ? timer(0, period) : of(0);
}

/** `interval(period)` in the browser; never emits on the server. */
export function tickEvery(period: number, isBrowser: boolean): Observable<number> {
  return isBrowser ? interval(period) : EMPTY;
}
