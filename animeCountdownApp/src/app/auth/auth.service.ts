import { computed, inject, Injectable, signal } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { User } from '../models/user.model';
import { Router } from '@angular/router';

// This service is meant to handle decifering token in storage if it changes or existant
@Injectable({ providedIn: 'root' })
export class AuthService {
  private tokenStorage = inject(TokenStorageService);
  private router = inject(Router);

  private storedToken = signal<string | null>(this.tokenStorage.getToken());
  readonly token = this.storedToken.asReadonly();

  private storedUser = signal<User | null>(
    this.decodeUserFromToken(this.storedToken())
  );
  readonly user = this.storedUser.asReadonly();

  readonly isAuthenticated = computed(() => this.storedUser() !== null);

  // Handles changes in storage
  constructor() {
    const token = sessionStorage.getItem('token');
    if (token) {
      this.storedToken.set(token);
      try {
        const decoded = this.decodeUserFromToken(token);
        this.storedUser.set(decoded);
      } catch {
        // Invalid token - clear it
        sessionStorage.removeItem('token');
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

  logout() {
    this.tokenStorage.clearToken();
    this.storedToken.set(null);
    this.storedUser.set(null);
    this.router.navigate([], { replaceUrl: true });
  }

}
