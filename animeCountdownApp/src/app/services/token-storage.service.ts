import { Injectable } from '@angular/core';

// The purpose of this service is to handle token setting, getting and deletion for a user session
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly KEY = 'accessToken';
  private readonly RKEY = 'refreshToken';

  setToken(token: string) {
    localStorage.setItem(this.KEY, token);
  }

  setRefreshToken(token: string) {
    localStorage.setItem(this.RKEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.RKEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.KEY);
  }

  clearTokens() {
    localStorage.removeItem(this.KEY);
    localStorage.removeItem(this.RKEY);
  }
}
