package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.RefreshTokenCookie;
import com.Toine.animeCountdownBackend.dto.AuthResponse;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(JwtService jwtService, UserRepository userRepository, RefreshTokenCookie refreshTokenCookie) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    /**
     * Exchanges the refresh token cookie for a new access token. The frontend calls this on page
     * load (to restore the session) and whenever an API call returns 401.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String refreshToken = refreshTokenCookie.read(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            // Signed-out visitors: every page load checks for a session, so "none" isn't an error
            // (a 401 here logged a console error on every page and cost Lighthouse points)
            return ResponseEntity.noContent().build();
        }

        try {
            String email = jwtService.extractUsername(refreshToken);
            Optional<UserEntity> user = email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email);

            if (user.isEmpty() || !jwtService.isRefreshTokenValid(refreshToken, user.get())) {
                return unauthorizedAndClearCookie();
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.get().getId());
            claims.put("name", user.get().getName());
            claims.put("picture", user.get().getPicture());

            return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(claims, user.get())));
        } catch (Exception e) {
            // Malformed, tampered or expired token
            return unauthorizedAndClearCookie();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                .build();
    }

    private ResponseEntity<String> unauthorizedAndClearCookie() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                .body("Invalid or expired session");
    }
}
