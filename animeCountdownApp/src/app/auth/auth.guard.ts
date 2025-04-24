import { inject } from "@angular/core";
import { CanActivateFn } from "@angular/router";
import { AuthService } from "./auth.service";
import { Router } from "express";


export const authGuard: CanActivateFn = () => {
    const user = inject(AuthService).user;
    const router = inject(Router);

    if (!user) {
        router.navigate(['/']);
        return false;
    }

    return true;
};