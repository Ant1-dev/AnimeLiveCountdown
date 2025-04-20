package com.Toine.animeCountdownBackend.components;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessComponent implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    String frontendUrl;

    public OAuth2SuccessComponent(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                email = attributes.get("email").toString();
                name = attributes.get("name").toString();
                picture = attributes.get("picture").toString();
                break;
            case "discord":
                email = attributes.get("email") != null ? attributes.get("email").toString() : "";
                name = attributes.get("username").toString();
                picture = "https://cdn.discordapp.com/avatars/" +
                          attributes.get("id").toString() + "/" +
                          attributes.get("avatar").toString() + ".png";
                break;
            case "github":
                email = attributes.get("email") != null ? attributes.get("email").toString() : "";
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

        response.sendRedirect(frontendUrl);
    }
}
