package com.Toine.animeCountdownBackend.dto;

public class AuthResponse {
    public String accessToken;

    public AuthResponse() {}

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
