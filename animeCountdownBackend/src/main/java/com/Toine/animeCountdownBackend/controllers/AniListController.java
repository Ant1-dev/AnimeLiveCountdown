package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.anilist.AniListAccountService;
import com.Toine.animeCountdownBackend.anilist.AniListAccountService.AniListUnavailableException;
import com.Toine.animeCountdownBackend.anilist.AniListFlowCookie;
import com.Toine.animeCountdownBackend.anilist.AniListOAuthSettings;
import com.Toine.animeCountdownBackend.components.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AniList connect / import for the signed-in user. */
@RestController
@RequestMapping("/api/auth/anilist")
public class AniListController {
    private final AniListAccountService service;
    private final AniListOAuthSettings settings;
    private final AniListFlowCookie flowCookie;

    public AniListController(AniListAccountService service, AniListOAuthSettings settings, AniListFlowCookie flowCookie) {
        this.service = service;
        this.settings = settings;
        this.flowCookie = flowCookie;
    }

    /** Whether AniList connect is available on this server and whether this user has connected. */
    @GetMapping
    public AniListAccountService.Status status() {
        return service.status(CurrentUser.require());
    }

    /** Starts the OAuth flow. The browser then navigates to the returned authorizeUrl. */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {
        AniListAccountService.ConnectStart start = service.startConnect(CurrentUser.require(), settings.redirectUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, flowCookie.create(start.nonce()))
                .body(Map.of("authorizeUrl", start.authorizeUrl()));
    }

    /** Imports (or re-syncs) the user's AniList CURRENT and PLANNING lists. */
    @PostMapping("/import")
    public AniListAccountService.ImportResult importList() {
        return service.importList(CurrentUser.require());
    }

    /** Unlinks AniList. Follows and progress stay. */
    @DeleteMapping
    public ResponseEntity<Void> disconnect() {
        service.disconnect(CurrentUser.require());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AniListUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(AniListUnavailableException e) {
        HttpStatus status = switch (e.reason()) {
            case NOT_CONFIGURED -> HttpStatus.NOT_FOUND;
            case NOT_CONNECTED, TOKEN_INVALID -> HttpStatus.CONFLICT;
            case COOLDOWN -> HttpStatus.TOO_MANY_REQUESTS;
            case ANILIST_ERROR -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage(), "reason", e.reason().name()));
    }
}
