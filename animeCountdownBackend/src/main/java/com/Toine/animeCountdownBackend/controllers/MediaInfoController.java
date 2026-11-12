package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import com.Toine.animeCountdownBackend.services.watchorder.WatchOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/info")
public class MediaInfoController {

    private final MediaInfoRepository mediaInfoRepository;
    private final WatchOrderService watchOrderService;
    private static final Pattern BANNER_PATTERN = Pattern.compile("/([^/]+)\\.jpg$");

    // Image dimension cache - avoid modifying entity
    private static final Map<String, int[]> IMAGE_DIMENSIONS = new HashMap<>();

    static {
        // Format: imageId -> [width, height]
        IMAGE_DIMENSIONS.put("21-wf37VakJmZqs", new int[]{1920, 400});
        // can add more as needed, this is just a hacky cache for now
    }

    public MediaInfoController (MediaInfoRepository mediaInfoRepository, WatchOrderService watchOrderService) {
        this.mediaInfoRepository = mediaInfoRepository;
        this.watchOrderService = watchOrderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMediaInfoById(@PathVariable Long id) {
        var media = mediaInfoRepository.findByIdWithGenres(id);
        if (media.isPresent()) {
            return ResponseEntity.ok(media.get());
        }
        // The explicit body lets the site tell "no such show" (404 page) from "API unreachable" (503)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "not_found"));
    }

    /** The franchise in release order (main story plus side stories), built from AniList relations. */
    @GetMapping("/{id}/watch-order")
    public ResponseEntity<?> getWatchOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                    .body(watchOrderService.watchOrder(id));
        } catch (RuntimeException e) {
            // AniList unreachable or rate limiting: the page just hides the section
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "300").build();
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<List<MediaInfoEntity>> getTrending() {
        Pageable pageable = PageRequest.of(0, 7);
        List<MediaInfoEntity> media = mediaInfoRepository.findAllByPopularityWithAiringDate(pageable).getContent();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                .body(media);
    }
}
