package com.Toine.animeCountdownBackend.components;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in user set by JwtAuthFilter. */
public final class CurrentUser {
    private CurrentUser() {
    }

    /** @throws ResponseStatusException 401 when the request isn't signed in */
    public static UserEntity require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        }
        return user;
    }
}
