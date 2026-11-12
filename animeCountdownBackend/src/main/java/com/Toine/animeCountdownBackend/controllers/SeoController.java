package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Search-engine support. The site's sitemap.xml and robots.txt are served by the frontend
 * (animeCountdownApp); this API only feeds it data and keeps crawlers off the API host.
 */
@RestController
public class SeoController {
    private final MediaInfoRepository mediaInfoRepository;

    public SeoController(MediaInfoRepository mediaInfoRepository) {
        this.mediaInfoRepository = mediaInfoRepository;
    }

    /** Every show's id and titles; the frontend turns them into /anime/:id/:slug sitemap entries. */
    @GetMapping("/api/sitemap/media")
    public ResponseEntity<List<MediaInfoRepository.SitemapEntry>> sitemapMedia() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(mediaInfoRepository.findSitemapEntries());
    }

    /**
     * robots.txt for the API host (api.animelivecountdown.com). API responses aren't pages, so
     * crawlers are kept out, except the share pages and card images link-preview bots fetch
     * (some, like Twitterbot, honour robots.txt).
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body("User-agent: *\nAllow: /api/share/\nDisallow: /\n");
    }
}
