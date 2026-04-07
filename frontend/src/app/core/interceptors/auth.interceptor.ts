import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

let isRefreshing = false;

function isPublicUrl(url: string): boolean {
  return url.includes('/auth/') || url.includes('/api/v1/invite/');
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(StorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = storage.getAccessToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isPublicUrl(req.url)) {
        return throwError(() => error);
      }

      // 403 on protected endpoint = removed from workspace → logout
      if (error.status === 403) {
        authService.clearAuth();
        router.navigate(['/auth/login']);
        return throwError(() => error);
      }

      // 401 on protected endpoint = token expired → try refresh
      if (error.status === 401) {
        return handleRefresh(req, next, authService, storage, router);
      }

      return throwError(() => error);
    })
  );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function handleRefresh(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  storage: StorageService,
  router: Router
) {
  if (isRefreshing) {
    return throwError(() => new Error('Refresh already in progress'));
  }

  isRefreshing = true;

  return authService.refresh().pipe(
    switchMap(response => {
      isRefreshing = false;
      return next(addToken(req, response.accessToken));
    }),
    catchError(err => {
      isRefreshing = false;
      authService.clearAuth();
      router.navigate(['/auth/login']);
      return throwError(() => err);
    })
  );
}
