import { HttpEvent, HttpHandler, HttpHandlerFn, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { AuthService } from "./auth.service";
import { Observable } from "rxjs";

// This purpose of this function is to reconstruct the response back to the backend with auth headers
export function AuthInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
    const token = inject(AuthService).token;

    const authReq = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authReq);
}