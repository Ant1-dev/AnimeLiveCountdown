package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.dto.ReactionSummary;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.services.ReactionService;
import jakarta.validation.constraints.Positive;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Episode reactions. Reading counts is public (under /api/reactions); changing your own reaction
 * requires sign-in (under /api/auth/reactions, which SecurityConfig already protects).
 */
@RestController
@Validated
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @GetMapping("/api/reactions/{mediaId}")
    public ResponseEntity<ReactionSummary> latest(@PathVariable @Positive Long mediaId) {
        return noStore(reactionService.latestSummary(mediaId, currentUser()));
    }

    @GetMapping("/api/reactions/{mediaId}/{episode}")
    public ResponseEntity<ReactionSummary> forEpisode(@PathVariable @Positive Long mediaId,
                                                      @PathVariable int episode) {
        return noStore(reactionService.summary(mediaId, episode, currentUser()));
    }

    @PutMapping("/api/auth/reactions/{mediaId}/{episode}")
    public ResponseEntity<ReactionSummary> react(@PathVariable @Positive Long mediaId,
                                                 @PathVariable int episode,
                                                 @RequestBody Map<String, String> body) {
        String reaction = body == null ? null : body.get("reaction");
        try {
            return noStore(reactionService.react(currentUser(), mediaId, episode, reaction));
        } catch (DataIntegrityViolationException raced) {
            // Two requests from the same user inserted at once and the other one won
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reaction changed at the same time, try again");
        }
    }

    @DeleteMapping("/api/auth/reactions/{mediaId}/{episode}")
    public ResponseEntity<ReactionSummary> remove(@PathVariable @Positive Long mediaId,
                                                  @PathVariable int episode) {
        return noStore(reactionService.remove(currentUser(), mediaId, episode));
    }

    private static ResponseEntity<ReactionSummary> noStore(ReactionSummary summary) {
        // Includes the viewer's own reaction, so it must not be cached by shared caches
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(summary);
    }

    private static UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity user)) {
            return null;
        }
        return user;
    }
}
