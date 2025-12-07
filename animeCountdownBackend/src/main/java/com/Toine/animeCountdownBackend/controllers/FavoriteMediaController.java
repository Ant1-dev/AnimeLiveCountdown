package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import jakarta.validation.constraints.Positive;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/favorite")
@Validated
public class FavoriteMediaController {
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final FavoriteMediaRepository favRepository;

    public FavoriteMediaController(FavoriteMediaRepository favRepository, MediaRepository mediaRepository, UserRepository userRepository) {
        this.favRepository = favRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<MediaEntity>> getAllFavoriteMedia(
            @PathVariable @Positive(message = "User ID must be positive") Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        List<MediaEntity> favorites = favRepository.findAllFavMedia(userId);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/{userId}/{mediaId}")
    public ResponseEntity<Map<String, String>> addFavoriteMedia(
            @PathVariable @Positive(message = "User ID must be positive") Long userId,
            @PathVariable @Positive(message = "Media ID must be positive") Long mediaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication required");
            return ResponseEntity.status(401).body(errorResponse);
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<MediaEntity> mediaOpt = mediaRepository.findById(mediaId);

        Map<String, String> response = new HashMap<>();

        if (userOpt.isEmpty() || mediaOpt.isEmpty()) {
            response.put("error", "User or media not found");
            return ResponseEntity.badRequest().body(response);
        }

        FavoriteMediaEntity favorite = new FavoriteMediaEntity();
        favorite.setUser(userOpt.get());
        favorite.setMedia(mediaOpt.get());
        favorite.setAddedDate(Instant.now());

        try {
            favRepository.save(favorite);
            response.put("message", "Successfully added to favorites!");
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException e) {
            response.put("error", "Media is already in favorites");
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            response.put("error", "An error occurred while adding to favorites");
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<String> deleteFavoriteMedia(
            @PathVariable @Positive(message = "Media ID must be positive") Long mediaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        int success = favRepository.removeFavoriteMedia(mediaId);

        if (success > 0)
            return ResponseEntity.ok("Successfully removed from favorites!");
        else
            return ResponseEntity.badRequest().body("Could not remove from favorites.");
    }
}