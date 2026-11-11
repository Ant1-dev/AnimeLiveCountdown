package com.Toine.animeCountdownBackend.components;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds and reads the refresh token cookie.
 *
 * The refresh token lives only in an HttpOnly cookie so page scripts can never read it, and it is
 * scoped to /api/auth so the browser sends it only to the refresh and logout endpoints.
 * SameSite=Lax works because the frontend and API share a site (animelivecountdown.com and
 * api.animelivecountdown.com; localhost:4200 and localhost:8080 in development).
 */
@Component
public class RefreshTokenCookie {
    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    @Value("${app.auth.cookie-secure:true}")
    private boolean secure;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public String create(String refreshToken) {
        return build(refreshToken, Duration.ofMillis(refreshExpirationMs)).toString();
    }

    public String clear() {
        return build("", Duration.ZERO).toString();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
