package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.anilist.AniListAccountService;
import com.Toine.animeCountdownBackend.anilist.AniListFlowCookie;
import com.Toine.animeCountdownBackend.anilist.AniListOAuthSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * AniList redirects here after the user approves (or declines) access. Always ends with a redirect
 * back to the Following page with ?anilist=connected|denied|error; the page then runs the import.
 * Served by AniListCallbackSecurityConfig's filter chain, not Spring's OAuth2 login.
 */
@RestController
public class AniListCallbackController {
    private static final Logger logger = LoggerFactory.getLogger(AniListCallbackController.class);

    private final AniListAccountService service;
    private final AniListOAuthSettings settings;
    private final AniListFlowCookie flowCookie;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AniListCallbackController(AniListAccountService service, AniListOAuthSettings settings,
                                     AniListFlowCookie flowCookie) {
        this.service = service;
        this.settings = settings;
        this.flowCookie = flowCookie;
    }

    @GetMapping(AniListOAuthSettings.CALLBACK_PATH)
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         HttpServletRequest request) {
        String outcome;
        if (!settings.isEnabled()) {
            outcome = "error";
        } else if (error != null) {
            outcome = "denied";
        } else {
            try {
                boolean linked = service.completeConnect(code, state, flowCookie.read(request), settings.redirectUri());
                outcome = linked ? "connected" : "error";
                if (!linked) {
                    logger.warn("AniList callback rejected: invalid or expired state, or missing flow cookie");
                }
            } catch (RuntimeException e) {
                logger.warn("AniList connect failed: {}", e.getMessage());
                outcome = "error";
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/following?anilist=" + outcome))
                .header(HttpHeaders.SET_COOKIE, flowCookie.clear())
                .build();
    }
}
