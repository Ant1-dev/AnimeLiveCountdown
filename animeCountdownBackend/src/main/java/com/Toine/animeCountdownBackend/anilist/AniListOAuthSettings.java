package com.Toine.animeCountdownBackend.anilist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * AniList OAuth app credentials, read from ANILIST_CLIENT_ID / ANILIST_CLIENT_SECRET.
 * When either is missing the AniList connect feature is off: the status endpoint reports
 * enabled=false (the UI hides the button) and the connect endpoints answer 404.
 */
@Component
public class AniListOAuthSettings {
    public static final String AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize";
    public static final String TOKEN_URL = "https://anilist.co/api/v2/oauth/token";
    public static final String CALLBACK_PATH = "/api/login/oauth2/code/anilist";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUriOverride;
    private final String authorizeUrl;

    @org.springframework.beans.factory.annotation.Autowired
    public AniListOAuthSettings(
            @Value("${app.anilist.client-id:${ANILIST_CLIENT_ID:}}") String clientId,
            @Value("${app.anilist.client-secret:${ANILIST_CLIENT_SECRET:}}") String clientSecret,
            // Optional: the exact redirect URI registered on AniList. By default it's built from the
            // request (this API's own host, which honours X-Forwarded-* behind the proxy).
            @Value("${app.anilist.redirect-uri:${ANILIST_REDIRECT_URI:}}") String redirectUriOverride,
            // Overridable only so the flow can be exercised against a local stub
            @Value("${app.anilist.authorize-url:" + AUTHORIZE_URL + "}") String authorizeUrl) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.redirectUriOverride = redirectUriOverride == null ? "" : redirectUriOverride.trim();
        this.authorizeUrl = authorizeUrl == null || authorizeUrl.isBlank() ? AUTHORIZE_URL : authorizeUrl.trim();
    }

    /** Convenience for tests: real AniList authorize URL. */
    public AniListOAuthSettings(String clientId, String clientSecret, String redirectUriOverride) {
        this(clientId, clientSecret, redirectUriOverride, AUTHORIZE_URL);
    }

    public String authorizeUrl() {
        return authorizeUrl;
    }

    public boolean isEnabled() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    /** Must be called while handling a request (it reads the current host). */
    public String redirectUri() {
        if (!redirectUriOverride.isEmpty()) {
            return redirectUriOverride;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(CALLBACK_PATH).toUriString();
    }
}
