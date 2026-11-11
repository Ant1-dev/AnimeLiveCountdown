package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final long ACCESS_MS = 60_000L;          // 1 minute
    private static final long REFRESH_MS = 7 * 86_400_000L; // 7 days

    private JwtService jwtService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_MS);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_MS);
        jwtService.init();

        user = new UserEntity();
        user.setEmail("test@example.com");
    }

    @Test
    void refreshTokenUsesRefreshExpiration() {
        String refresh = jwtService.generateRefreshToken(user);
        Date expiry = jwtService.extractClaim(refresh, Claims::getExpiration);

        long remaining = expiry.getTime() - System.currentTimeMillis();
        assertTrue(remaining > ACCESS_MS, "refresh token should outlive the access token");
        assertTrue(remaining <= REFRESH_MS);
    }

    @Test
    void accessTokenUsesAccessExpiration() {
        String access = jwtService.generateToken(user);
        Date expiry = jwtService.extractClaim(access, Claims::getExpiration);

        assertTrue(expiry.getTime() - System.currentTimeMillis() <= ACCESS_MS);
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() {
        String refresh = jwtService.generateRefreshToken(user);

        assertFalse(jwtService.isTokenValid(refresh, user));
        assertTrue(jwtService.isRefreshTokenValid(refresh, user));
    }

    @Test
    void accessTokenIsNotAcceptedAsRefreshToken() {
        String access = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(access, user));
        assertFalse(jwtService.isRefreshTokenValid(access, user));
    }
}
