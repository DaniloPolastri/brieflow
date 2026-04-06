import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StorageService } from './storage.service';
import { LoginRequest, RegisterRequest, TokenResponse, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storage = inject(StorageService);

  private readonly apiUrl = `${environment.apiUrl}/api/v1/auth`;
  private readonly _currentUser = signal<User | null>(this.storage.getUser());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  register(request: RegisterRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  refresh(): Observable<TokenResponse> {
    const refreshToken = this.storage.getRefreshToken();
    return this.http.post<TokenResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.storage.getRefreshToken();
    return this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).pipe(
      tap(() => this.clearAuth())
    );
  }

  isAuthenticated(): boolean {
    return this.storage.getAccessToken() !== null;
  }

  clearAuth(): void {
    this.storage.clear();
    this._currentUser.set(null);
  }

  private handleAuthResponse(response: TokenResponse): void {
    this.storage.setAccessToken(response.accessToken);
    this.storage.setRefreshToken(response.refreshToken);
    this.storage.setUser(response.user);
    this._currentUser.set(response.user);
  }
}
