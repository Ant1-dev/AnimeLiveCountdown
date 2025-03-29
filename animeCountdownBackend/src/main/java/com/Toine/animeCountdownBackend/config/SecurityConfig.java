package com.Toine.animeCountdownBackend.config;

import com.Toine.animeCountdownBackend.components.OAuth2SuccessComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final OAuth2SuccessComponent oAuth2SuccessComponent;

    public SecurityConfig(OAuth2SuccessComponent oAuth2SuccessComponent) {
        this.oAuth2SuccessComponent = oAuth2SuccessComponent;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // disable CSRF for API requests
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().permitAll()  // allow client to serve its routes
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessComponent)
                )
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // redirect to client root after logout
                            response.sendRedirect("/");
                        })
                );

        return http.build();
    }
}