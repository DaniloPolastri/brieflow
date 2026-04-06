import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

export const roleGuard = (...allowedRoles: string[]): CanActivateFn => {
  return () => {
    const storage = inject(StorageService);
    const router = inject(Router);

    const user = storage.getUser();
    const userRole = (user as any)?.role;

    if (userRole && allowedRoles.includes(userRole)) {
      return true;
    }

    return router.createUrlTree(['/dashboard']);
  };
};
