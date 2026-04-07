import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError, Observable, isObservable } from 'rxjs';
import { firstValueFrom } from 'rxjs';
import { authGuard, resetAuthGuardState } from './auth.guard';
import { StorageService } from '../services/storage.service';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let storageService: StorageService;
  let router: Router;
  let authServiceSpy: { refresh: ReturnType<typeof vi.fn>; clearAuth: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authServiceSpy = {
      refresh: vi.fn().mockReturnValue(of({
        accessToken: 'new-token',
        refreshToken: 'new-refresh',
        expiresIn: 900,
        user: { id: 1, name: 'Test', email: 'test@test.com', workspaceId: 1, workspaceName: 'W', role: 'OWNER', position: 'DIRETOR_DE_ARTE' },
      })),
      clearAuth: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { createUrlTree: vi.fn((commands: string[]) => commands.join('/')) } },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });
    storageService = TestBed.inject(StorageService);
    router = TestBed.inject(Router);
    storageService.clear();
    resetAuthGuardState();
  });

  it('should redirect to login when no token', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(result).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should validate session on first access when token exists', async () => {
    storageService.setAccessToken('valid-token');
    storageService.setRefreshToken('valid-refresh');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    const val = isObservable(result) ? await firstValueFrom(result as Observable<any>) : result;
    expect(val).toBe(true);
    expect(authServiceSpy.refresh).toHaveBeenCalled();
  });

  it('should redirect to login when session validation fails', async () => {
    authServiceSpy.refresh.mockReturnValue(throwError(() => new Error('forbidden')));
    storageService.setAccessToken('invalid-token');
    storageService.setRefreshToken('invalid-refresh');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    const val = isObservable(result) ? await firstValueFrom(result as Observable<any>) : result;
    expect(authServiceSpy.clearAuth).toHaveBeenCalled();
    expect(router.createUrlTree).toHaveBeenCalledWith(['/auth/login']);
  });
});
