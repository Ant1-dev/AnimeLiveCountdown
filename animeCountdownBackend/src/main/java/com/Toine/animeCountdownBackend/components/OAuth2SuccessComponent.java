package com.Toine.animeCountdownBackend.components;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    public OAuth2SuccessComponent(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = attributes.get("email").toString();
        String name = attributes.get("name").toString();
        String picture = attributes.get("picture").toString();

        UserEntity user = userRepository.findByEmail(email)
                .orElse(new UserEntity());
        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);

        user = userRepository.save(user);

        response.sendRedirect("/api/auth/user/" + user.getId());
    }
}
