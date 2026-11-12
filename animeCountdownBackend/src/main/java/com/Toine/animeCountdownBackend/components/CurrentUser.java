package com.Toine.animeCountdownBackend.components;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in user that JwtAuthFilter put in the security context. */
public final class CurrentUser {
    private CurrentUser() {
    }

    /** The signed-in user, or null if the request is not authenticated. */
    public static UserEntity get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity user)) {
            return null;
        }
        return user;
    }

    /** @throws ResponseStatusException 401 when the request isn't signed in */
    public static UserEntity require() {
        UserEntity user = get();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        }
        return user;
    }
}
