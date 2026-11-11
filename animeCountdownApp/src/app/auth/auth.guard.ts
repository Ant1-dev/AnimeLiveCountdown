import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // On a fresh page load the session is restored asynchronously, so wait for that first
  return authService
    .whenSessionChecked()
    .pipe(map(() => (authService.isAuthenticated() ? true : router.createUrlTree(['/']))));
};
