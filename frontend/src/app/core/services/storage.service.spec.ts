import { StorageService } from './storage.service';

describe('StorageService', () => {
  let service: StorageService;

  beforeEach(() => {
    localStorage.clear();
    service = new StorageService();
  });

  it('should store and retrieve access token', () => {
    service.setAccessToken('test-token');
    expect(service.getAccessToken()).toBe('test-token');
  });

  it('should store and retrieve refresh token', () => {
    service.setRefreshToken('test-refresh');
    expect(service.getRefreshToken()).toBe('test-refresh');
  });

  it('should clear all tokens', () => {
    service.setAccessToken('token');
    service.setRefreshToken('refresh');
    service.clear();
    expect(service.getAccessToken()).toBeNull();
    expect(service.getRefreshToken()).toBeNull();
  });
});
