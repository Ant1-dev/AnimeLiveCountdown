package com.Toine.animeCountdownBackend.anilist;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Holds the connect flow's nonce on this browser between "Connect AniList" and AniList's redirect back.
 * HttpOnly, scoped to the callback path, and SameSite=Lax (sent on AniList's top-level redirect).
 */
@Component
public class AniListFlowCookie {
    public static final String NAME = "anilist_oauth";

    @Value("${app.auth.cookie-secure:true}")
    private boolean secure;

    public String create(String nonce) {
        return build(nonce, AniListOAuthState.LIFETIME).toString();
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
                .path(AniListOAuthSettings.CALLBACK_PATH)
                .maxAge(maxAge)
                .build();
    }
}
