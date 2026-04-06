import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { StorageService } from '../services/storage.service';

describe('authGuard', () => {
  let storageService: StorageService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { createUrlTree: vi.fn((commands: string[]) => commands.join('/')) } },
      ],
    });
    storageService = TestBed.inject(StorageService);
    router = TestBed.inject(Router);
    storageService.clear();
  });

  it('should allow access when token exists', () => {
    storageService.setAccessToken('valid-token');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(result).toBe(true);
  });

  it('should redirect to login when no token', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(result).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/auth/login']);
  });
});
