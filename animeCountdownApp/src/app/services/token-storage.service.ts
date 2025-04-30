import { Injectable } from "@angular/core";

// The purpose of this service is to handle token setting, getting and deletion for a user session
@Injectable({providedIn: 'root'})
export class TokenStorageService {
    private readonly KEY = 'auth_token';

    saveToken(token: string) {
        localStorage.setItem(this.KEY, token);
    }

    getToken(): string | null {
        return localStorage.getItem(this.KEY);
    }

    clearToken() {
        localStorage.removeItem(this.KEY);
    }
}