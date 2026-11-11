import { HttpErrorResponse, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

const AUTH_ENDPOINTS = ['auth/refresh', 'auth/logout'];

/**
 * Adds the in-memory access token to API requests. When the API answers 401 for a signed-in user,
 * refreshes the access token once (via the refresh token cookie) and retries the request.
 */
export function AuthInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const authService = inject(AuthService);

  // Only our own API gets the token, and the refresh/logout calls authenticate by cookie
  const isApiRequest = req.url.startsWith(environment.apiUrl);
  const isAuthEndpoint = AUTH_ENDPOINTS.some((path) => req.url.startsWith(environment.apiUrl + path));
  if (!isApiRequest || isAuthEndpoint) {
    return next(req);
  }

  const token = authService.accessToken();
  const authReq = token && !authService.isTokenExpired(token) ? withToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !authService.isAuthenticated()) {
        return throwError(() => error);
      }

      // If the refresh fails the session is gone; refresh() has already cleared it
      return authService.refresh().pipe(switchMap((newToken) => next(withToken(req, newToken))));
    })
  );
}

function withToken(req: HttpRequest<unknown>, token: string) {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
