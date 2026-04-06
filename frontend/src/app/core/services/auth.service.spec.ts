import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { StorageService } from './storage.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let storageService: StorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    storageService = TestBed.inject(StorageService);
    storageService.clear();
  });

  afterEach(() => httpMock.verify());

  it('should login and store tokens', () => {
    const mockResponse = {
      accessToken: 'access-123',
      refreshToken: 'refresh-456',
      expiresIn: 900000,
      user: { id: 1, name: 'John', email: 'john@test.com' },
    };

    service.login({ email: 'john@test.com', password: 'pass' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(storageService.getAccessToken()).toBe('access-123');
    expect(storageService.getRefreshToken()).toBe('refresh-456');
    expect(service.currentUser()?.email).toBe('john@test.com');
  });

  it('should register and store tokens', () => {
    const mockResponse = {
      accessToken: 'access-123',
      refreshToken: 'refresh-456',
      expiresIn: 900000,
      user: { id: 1, name: 'John', email: 'john@test.com' },
    };

    service.register({ name: 'John', email: 'john@test.com', password: 'pass', workspaceName: 'Test Workspace' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(service.currentUser()?.name).toBe('John');
  });

  it('should clear tokens on logout', () => {
    storageService.setAccessToken('token');
    storageService.setRefreshToken('refresh');

    service.logout().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/logout');
    req.flush(null);

    expect(storageService.getAccessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('should report not authenticated when no token', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should report authenticated when token exists', () => {
    storageService.setAccessToken('some-token');
    expect(service.isAuthenticated()).toBe(true);
  });
});
