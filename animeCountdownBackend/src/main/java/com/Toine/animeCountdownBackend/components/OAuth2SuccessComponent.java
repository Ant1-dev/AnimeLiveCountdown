package com.Toine.animeCountdownBackend.components;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.services.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessComponent implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final FetchGithubEmail githubEmailFetcher;
    private final JwtService jwtService;
    private final RefreshTokenCookie refreshTokenCookie;

    @Value("${app.frontend.url}")
    String frontendUrl;


    public OAuth2SuccessComponent(UserRepository userRepository, OAuth2AuthorizedClientService authorizedClientService, FetchGithubEmail githubEmailFetcher, JwtService jwtService, RefreshTokenCookie refreshTokenCookie) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
        this.githubEmailFetcher = githubEmailFetcher;
        this.jwtService = jwtService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email;
        String name;
        String picture;

        switch (registrationId) {
            case "google":
                email = attributes.get("email") != null ? attributes.get("email").toString() : "";
                name = attributes.get("name") != null ? attributes.get("name").toString() : "Google User";
                picture = attributes.get("picture") != null ? attributes.get("picture").toString() : "";
                break;
            case "discord":
                email = attributes.get("email") != null ? attributes.get("email").toString() : "";
                name = attributes.get("username") != null ? attributes.get("username").toString() : "Discord User";
                Object discordId = attributes.get("id");
                Object discordAvatar = attributes.get("avatar");
                picture = (discordId != null && discordAvatar != null)
                    ? "https://cdn.discordapp.com/avatars/" + discordId.toString() + "/" + discordAvatar.toString() + ".png"
                    : "";
                break;
            case "github":
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken.getName()
                );
                String accessToken = client.getAccessToken().getTokenValue();

                // Fetch private email
                email = githubEmailFetcher.getPrimaryEmail(accessToken);
                name = attributes.get("login") != null ? attributes.get("login").toString() : "Unknown";
                picture = attributes.get("avatar_url") != null ? attributes.get("avatar_url").toString() : "";
                break;


            default:
                email = attributes.get("email") != null ? attributes.get("email").toString() : "";
                name = attributes.get("name") != null ? attributes.get("name").toString() : "User";
                picture = "";
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElse(new UserEntity());
        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);

        user = userRepository.save(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // The refresh token goes in an HttpOnly cookie rather than the URL, so it never lands in
        // browser history, logs or Referer headers. The frontend gets an access token by calling
        // /api/auth/refresh on load.
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(refreshToken));
        response.sendRedirect(frontendUrl);
    }
}
