package com.Toine.animeCountdownBackend.config;

import com.Toine.animeCountdownBackend.anilist.AniListOAuthSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The AniList OAuth callback lives under /api/login/oauth2/code/ (the redirect URI registered on
 * AniList), which the main chain hands to Spring's OAuth2 login filter. AniList connect runs its own
 * flow (see AniListCallbackController), so this chain claims just that one path first and lets the
 * request reach the controller. The controller checks the signed state and the flow cookie itself.
 */
@Configuration
public class AniListCallbackSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain aniListCallbackChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AniListOAuthSettings.CALLBACK_PATH)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
