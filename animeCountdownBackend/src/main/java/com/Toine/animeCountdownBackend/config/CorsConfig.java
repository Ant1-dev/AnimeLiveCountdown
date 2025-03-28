package com.Toine.animeCountdownBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // specific origins:
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("https://animelivecountdown.com");

        // allow all HTTP methods
        config.addAllowedMethod("*");

        // allow all headers
        config.addAllowedHeader("*");

        // allow credentials like cookies, authentication in the future???
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}