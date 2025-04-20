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
        config.addAllowedOrigin("http://127.0.0.1:8081");
        config.addAllowedOrigin("https://anime-live-countdown-6y6pbb7ma-ant1-devs-projects.vercel.app/"); // commits
        config.addAllowedOrigin("https://anime-live-countdown-git-main-ant1-devs-projects.vercel.app/"); //latest dev on main

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