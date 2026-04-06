import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { roleGuard } from './role.guard';
import { StorageService } from '../services/storage.service';

describe('roleGuard', () => {
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

  it('should allow access when user has an allowed role', () => {
    storageService.setUser({ id: 1, name: 'Ana', email: 'ana@test.com', role: 'OWNER' } as any);

    const guard = roleGuard('OWNER', 'MANAGER');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('should allow access when user has any of multiple allowed roles', () => {
    storageService.setUser({ id: 2, name: 'Bruno', email: 'bruno@test.com', role: 'MANAGER' } as any);

    const guard = roleGuard('OWNER', 'MANAGER');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('should redirect to dashboard when user role is not in allowed list', () => {
    storageService.setUser({ id: 3, name: 'Carlos', email: 'carlos@test.com', role: 'CREATIVE' } as any);

    const guard = roleGuard('OWNER', 'MANAGER');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should redirect to dashboard when no user in storage', () => {
    const guard = roleGuard('OWNER');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
  });
});
