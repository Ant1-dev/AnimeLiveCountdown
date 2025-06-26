import { HttpHandlerFn, HttpRequest, HttpErrorResponse } from "@angular/common/http";
import { inject } from "@angular/core";
import { catchError, switchMap, throwError, of } from "rxjs";
import { AuthService } from "./auth.service";

// Track ongoing refresh to prevent multiple simultaneous refresh calls
let isRefreshing = false;

export function AuthInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const authService = inject(AuthService);
  
  // Skip auth for refresh endpoint to prevent infinite loops
  if (req.url.includes('/api/auth/refresh')) {
    return next(req);
  }

  const token = authService.accessToken();
  
  // Check if token is expired before sending it
  if (token && authService.isTokenExpired && authService.isTokenExpired(token)) {
    // Token is expired, don't send it
    return next(req);
  }
  
  // Add token to request if available and valid
  const authReq = token ? req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized errors
      if (error.status === 401 && token && !isRefreshing) {
        return handleUnauthorized(req, next, authService);
      }
      
      return throwError(() => error);
    })
  );
}

function handleUnauthorized(
  request: HttpRequest<unknown>, 
  next: HttpHandlerFn, 
  authService: AuthService
) {
  const refreshToken = authService.refreshToken();
  
  if (!refreshToken) {
    // No refresh token available, logout
    authService.logout();
    return throwError(() => new Error('No refresh token available'));
  }

  // Set refreshing flag to prevent multiple simultaneous refresh attempts
  isRefreshing = true;
  
  return authService.getRefreshTokenFromApi().pipe(
    switchMap((response) => {
      // Refresh successful - update tokens
      authService.setToken(response.accessToken, refreshToken);
      
      // Retry the original request with new access token
      const retryReq = request.clone({
        setHeaders: {
          Authorization: `Bearer ${response.accessToken}`
        }
      });
      
      isRefreshing = false;
      return next(retryReq);
    }),
    catchError((refreshError) => {
      // Refresh failed - logout user
      isRefreshing = false;
      authService.logout();
      return throwError(() => refreshError);
    })
  );
}