import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, tap } from 'rxjs';

// Simple in-memory cache
const cache = new Map<string, { response: HttpResponse<any>; timestamp: number }>();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

export const cacheInterceptor: HttpInterceptorFn = (req, next) => {
  // Only cache GET requests
  if (req.method !== 'GET') {
    return next(req);
  }

  // Don't cache auth-related requests
  if (req.url.includes('/auth/') || req.url.includes('/login/')) {
    return next(req);
  }

  const cachedEntry = cache.get(req.url);
  const now = Date.now();

  // Return cached response if still valid
  if (cachedEntry && now - cachedEntry.timestamp < CACHE_TTL) {
    return of(cachedEntry.response.clone());
  }

  // Make request and cache the response
  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        cache.set(req.url, {
          response: event.clone(),
          timestamp: now
        });

        // Clean up expired cache entries
        cleanExpiredCache();
      }
    })
  );
};

function cleanExpiredCache(): void {
  const now = Date.now();
  const keysToDelete: string[] = [];

  cache.forEach((value, key) => {
    if (now - value.timestamp >= CACHE_TTL) {
      keysToDelete.push(key);
    }
  });

  keysToDelete.forEach(key => cache.delete(key));
}
