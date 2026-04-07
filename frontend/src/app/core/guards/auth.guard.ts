import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { StorageService } from '../services/storage.service';
import { AuthService } from '../services/auth.service';

let sessionValidated = false;

/** @internal — for testing only */
export function resetAuthGuardState(): void {
  sessionValidated = false;
}

export const authGuard: CanActivateFn = () => {
  const storage = inject(StorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!storage.getAccessToken()) {
    return router.createUrlTree(['/auth/login']);
  }

  // Validates session once on page load. Subsequent protection relies on
  // the auth interceptor catching 403 responses on API calls.
  if (!sessionValidated) {
    sessionValidated = true;

    return authService.refresh().pipe(
      map(() => true),
      catchError(() => {
        authService.clearAuth();
        return of(router.createUrlTree(['/auth/login']));
      })
    );
  }

  return true;
};
