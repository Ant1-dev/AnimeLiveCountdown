import { computed, inject, Injectable, signal } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { User } from '../models/user.model';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

// This service is meant to handle decifering token in storage if it changes or existant
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private tokenStorage = inject(TokenStorageService);
  private router = inject(Router);

  private storedToken = signal<string | null>(this.tokenStorage.getToken());
  readonly token = this.storedToken.asReadonly();

  private storedUser = signal<User | null>(
    this.decodeUserFromToken(this.storedToken())
  );

  readonly user = this.storedUser.asReadonly();

  readonly isAuthenticated = computed(() => this.storedUser() !== null);

  constructor() {
    const token = this.tokenStorage.getToken();
    if (token && !this.isTokenExpired(token)) {
      this.storedToken.set(token);
      try {
        const decoded = this.decodeUserFromToken(token);
        this.storedUser.set(decoded);
      } catch {
        this.tokenStorage.clearToken();
        this.storedToken.set(null);
        this.storedUser.set(null);
      }
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

  refreshToken(): Observable<{accessToken: string}> {
    return this.http.post<{ accessToken: string }>('/api/auth/refresh', {});
  }

  setToken(token: string) {
    this.tokenStorage.saveToken(token);
    this.storedToken.set(token)
    const user = this.decodeUserFromToken(token);
    this.storedUser.set(user)
  }

  logout() {
    this.tokenStorage.clearToken();
    this.storedToken.set(null);
    this.storedUser.set(null);
    this.router.navigate(['/']);
  }
}
