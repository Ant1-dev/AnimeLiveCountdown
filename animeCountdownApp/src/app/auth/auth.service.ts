import { computed, inject, Injectable, signal } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { User } from '../models/user.model';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

// This service is meant to handle decifering tokens in storage if it changes or existant
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private tokenStorage = inject(TokenStorageService);
  private router = inject(Router);

  private storedAccessToken = signal<string | null>(
    this.tokenStorage.getToken()
  );
  private storedRefreshToken = signal<string | null>(
    this.tokenStorage.getRefreshToken()
  );
  readonly accessToken = this.storedAccessToken.asReadonly();
  readonly refreshToken = this.storedRefreshToken.asReadonly();

  private storedUser = signal<User | null>(
    this.decodeUserFromToken(this.storedAccessToken())
  );

  readonly user = this.storedUser.asReadonly();

  readonly isAuthenticated = computed(() => this.storedUser() !== null);

  constructor() {
    const accessToken = this.tokenStorage.getToken();
    const refreshToken = this.tokenStorage.getRefreshToken();

    if (accessToken && !this.isTokenExpired(accessToken) && refreshToken) {
      this.storedAccessToken.set(accessToken);
      this.storedRefreshToken.set(refreshToken);
      try {
        const decoded = this.decodeUserFromToken(accessToken);
        this.storedUser.set(decoded);
      } catch {
        this.tokenStorage.clearTokens();
        this.storedAccessToken.set(null);
        this.storedUser.set(null);
        this.storedRefreshToken.set(null);
      }
    } else if (refreshToken && !this.isTokenExpired(refreshToken)) {
      this.storedRefreshToken.set(refreshToken);
    }
  }

  private decodeUserFromToken(token: string | null): User | null {
    if (!token) return null;

    try {
      const payload = token.split('.')[1];
      const json = atob(payload);
      const data = JSON.parse(json);
      return {
        id: data.id,
        email: data.sub,
        name: data.name,
        picture: data.picture,
      };
    } catch (err) {
      console.error('Failed to decode token:', err);
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      const json = atob(payload);
      const data = JSON.parse(json);
      const currentTime = Math.floor(Date.now() / 1000);
      return data.exp < currentTime;
    } catch {
      return true;
    }
  }

  getRefreshTokenFromApi(): Observable<{ accessToken: string }> {
    const refreshToken = this.refreshToken();
    return this.http.post<{ accessToken: string }>('/api/auth/refresh', {
      refreshToken: refreshToken
    });
  }

  setToken(accessToken: string, refreshToken: string) {
    this.tokenStorage.setToken(accessToken);
    this.tokenStorage.setRefreshToken(refreshToken);

    this.storedAccessToken.set(accessToken);
    this.storedRefreshToken.set(refreshToken);

    const user = this.decodeUserFromToken(accessToken);
    this.storedUser.set(user);
  }

  logout() {
    this.tokenStorage.clearTokens();
    this.storedAccessToken.set(null);
    this.storedRefreshToken.set(null);
    this.storedUser.set(null);
    this.router.navigate(['/']);
  }
}
