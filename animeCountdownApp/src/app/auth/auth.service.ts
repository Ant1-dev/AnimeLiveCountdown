import { computed, inject, Injectable, signal } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { User } from '../models/user.model';
import { Router, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Location } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);
  private router = inject(Router);
  private location = inject(Location);

  private storedAccessToken = signal<string | null>(null);
  private storedRefreshToken = signal<string | null>(null);

  readonly accessToken = this.storedAccessToken.asReadonly();
  readonly refreshToken = this.storedRefreshToken.asReadonly();

  private storedUser = signal<User | null>(null);
  readonly user = this.storedUser.asReadonly();
  readonly isAuthenticated = computed(() => this.storedUser() !== null);

  constructor() {
    try {
      this.initializeTokens();
    } catch (error) {
      console.error('Error initializing tokens:', error);
      this.clearTokens();
    }
  }

  private initializeTokens(): void {
    try {
      // check URL parameters for OAuth2 tokens
      this.handleOAuth2Tokens();

      // Then check stored tokens
      this.loadStoredTokens();
    } catch (error) {
      console.error('Error in initializeTokens:', error);
      this.clearTokens();
    }
  }
  private handleOAuth2Tokens(): void {
    // Get URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const urlAccessToken = urlParams.get('accessToken');
    const urlRefreshToken = urlParams.get('refreshToken');

    if (urlAccessToken && urlRefreshToken) {
      // Validate tokens before storing
      if (
        !this.isTokenExpired(urlAccessToken) &&
        !this.isTokenExpired(urlRefreshToken)
      ) {
        // Store the tokens
        this.tokenStorage.setToken(urlAccessToken);
        this.tokenStorage.setRefreshToken(urlRefreshToken);

        // Update signals
        this.storedAccessToken.set(urlAccessToken);
        this.storedRefreshToken.set(urlRefreshToken);

        // Decode and set user
        try {
          const user = this.decodeUserFromToken(urlAccessToken);
          this.storedUser.set(user);
        } catch (error) {
          console.error('Failed to decode user from OAuth2 token:', error);
          this.clearTokens();
        }
      } else {
        console.warn('OAuth2 tokens are expired');
      }

      // Clean the URL (remove tokens from browser history for security)
      this.cleanUrl();
    }
  }

  private loadStoredTokens(): void {
    // Only load from storage if don't already have tokens from URL
    if (!this.storedAccessToken()) {
      const accessToken = this.tokenStorage.getToken();
      const refreshToken = this.tokenStorage.getRefreshToken();

      if (accessToken && !this.isTokenExpired(accessToken) && refreshToken) {
        this.storedAccessToken.set(accessToken);
        this.storedRefreshToken.set(refreshToken);

        try {
          const decoded = this.decodeUserFromToken(accessToken);
          this.storedUser.set(decoded);
        } catch {
          this.clearTokens();
        }
      } else if (refreshToken && !this.isTokenExpired(refreshToken)) {
        // Access token expired but refresh token valid
        this.storedRefreshToken.set(refreshToken);
        // Could auto-refresh here, but interceptor will handle it
      } else {
        // Clear any invalid stored tokens
        this.clearTokens();
      }
    }
  }

  private cleanUrl(): void {
    // Get current URL without query parameters
    const urlWithoutParams = window.location.pathname;

    // Update browser history without the token parameters
    this.location.replaceState(urlWithoutParams);
  }

  private clearTokens(): void {
    this.tokenStorage.clearTokens();
    this.storedAccessToken.set(null);
    this.storedRefreshToken.set(null);
    this.storedUser.set(null);
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

  public isTokenExpired(token: string): boolean {
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
      refreshToken: refreshToken,
    });
  }

  setToken(accessToken: string, refreshToken: string): void {
    this.tokenStorage.setToken(accessToken);
    this.tokenStorage.setRefreshToken(refreshToken);
    this.storedAccessToken.set(accessToken);
    this.storedRefreshToken.set(refreshToken);
    const user = this.decodeUserFromToken(accessToken);
    this.storedUser.set(user);
  }

  logout(): void {
    this.clearTokens();
    this.storedAccessToken.set(null);
    this.storedRefreshToken.set(null);
    this.storedUser.set(null);
    this.router.navigate(['/']);
  }
}
