package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/favorite")
public class FavoriteMediaController {
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private FavoriteMediaRepository favRepository;

    public FavoriteMediaController(FavoriteMediaRepository favRepository, MediaRepository mediaRepository, UserRepository userRepository) {
        this.favRepository = favRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public List<MediaEntity> getAllFavoriteMedia(@PathVariable Long userId) {
        return favRepository.findAllFavMedia(userId);
    }

    @PostMapping("/{userId}/{mediaId}")
    public ResponseEntity<String> addFavoriteMedia(@PathVariable Long userId, @PathVariable Long mediaId) {
        Optional<UserEntity> user = userRepository.findById(userId);
        Optional<MediaEntity> media = mediaRepository.findById(mediaId);

        boolean success = favRepository.addFavoriteMedia(user, media, Instant.now());
        if (success)
            return ResponseEntity.ok("Successfully added to favorites!");
        else
            return ResponseEntity.badRequest().body("Could not be added to favorites.");
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<String> deleteFavoriteMedia(@PathVariable Long mediaId) {
        boolean success = favRepository.removeFavoriteMedia(mediaId);

        if (success)
            return ResponseEntity.ok("Successfully removed from favorites!");
        else
            return ResponseEntity.badRequest().body("Could not remove from favorites.");
    }
}
