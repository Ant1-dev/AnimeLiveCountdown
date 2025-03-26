package com.Toine.animeCountdownBackend.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/api/auth/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        return Map.of(
                "name", principal.getAttribute("name"),
                "email", principal.getAttribute("email"),
                "picture", principal.getAttribute("picture"),
                "sub", principal.getAttribute("sub")
        );
    }

    @GetMapping("/api/auth/user/details")
    public Map<String, Object> getUserDetails(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }
}