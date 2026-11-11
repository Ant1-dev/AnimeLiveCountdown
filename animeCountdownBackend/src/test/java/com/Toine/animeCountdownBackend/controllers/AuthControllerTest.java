package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.RefreshTokenCookie;
import com.Toine.animeCountdownBackend.dto.AuthResponse;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.services.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final long REFRESH_MS = 7 * 86_400_000L;

    private JwtService jwtService;
    private RefreshTokenCookie refreshTokenCookie;
    private AuthController controller;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", Base64.getEncoder().encodeToString(new byte[32]));
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_MS);
        jwtService.init();

        refreshTokenCookie = new RefreshTokenCookie();
        ReflectionTestUtils.setField(refreshTokenCookie, "secure", true);
        ReflectionTestUtils.setField(refreshTokenCookie, "refreshExpirationMs", REFRESH_MS);

        user = new UserEntity();
        user.setId(42L);
        user.setEmail("test@example.com");
        user.setName("Tester");

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        controller = new AuthController(jwtService, userRepository, refreshTokenCookie);
    }

    @Test
    void refreshWithValidCookieReturnsAccessToken() {
        ResponseEntity<?> response = controller.refreshToken(requestWithCookie(jwtService.generateRefreshToken(user)));

        assertEquals(200, response.getStatusCode().value());
        String accessToken = ((AuthResponse) response.getBody()).getAccessToken();
        assertTrue(jwtService.isTokenValid(accessToken, user));
        Integer id = jwtService.extractClaim(accessToken, c -> c.get("id", Integer.class));
        assertEquals(42, id.intValue());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() {
        ResponseEntity<?> response = controller.refreshToken(new MockHttpServletRequest());

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void accessTokenInCookieIsRejectedAndCookieCleared() {
        ResponseEntity<?> response = controller.refreshToken(requestWithCookie(jwtService.generateToken(user)));

        assertEquals(401, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
    }

    @Test
    void garbageCookieIsRejected() {
        ResponseEntity<?> response = controller.refreshToken(requestWithCookie("not-a-jwt"));

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void logoutClearsCookie() {
        String setCookie = controller.logout().getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertTrue(setCookie.startsWith(RefreshTokenCookie.NAME + "="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("Path=/api/auth"));
    }

    @Test
    void refreshCookieIsHttpOnlySecureLax() {
        String cookie = refreshTokenCookie.create("abc");

        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api/auth"));
        assertTrue(cookie.contains("Max-Age=" + REFRESH_MS / 1000));
    }

    private MockHttpServletRequest requestWithCookie(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RefreshTokenCookie.NAME, value));
        return request;
    }
}
