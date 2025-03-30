package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/info")
public class MediaInfoController {

    private final MediaInfoRepository mediaInfoRepository;
    private static final Pattern BANNER_PATTERN = Pattern.compile("/([^/]+)\\.jpg$");

    // Image dimension cache - avoid modifying entity
    private static final Map<String, int[]> IMAGE_DIMENSIONS = new HashMap<>();

    static {
        // Format: imageId -> [width, height]
        IMAGE_DIMENSIONS.put("21-wf37VakJmZqs", new int[]{1920, 400});
        // can add more as needed, this is just a hacky cache for now
    }

    public MediaInfoController (MediaInfoRepository mediaInfoRepository) {
        this.mediaInfoRepository = mediaInfoRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaInfoEntity> getMediaInfoById(@PathVariable Long id) {
        return mediaInfoRepository.findByIdWithGenres(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trending")
    @Cacheable(value = "trendingCache", key = "'trending'")
    public ResponseEntity<Map<String, Object>> getTrending() {
        Pageable pageable = PageRequest.of(0, 7);
        List<MediaInfoEntity> media = mediaInfoRepository.findAllByPopularityWithAiringDate(pageable).getContent();

        // Create enhanced response with dimensions
        Map<String, Object> response = new HashMap<>();
        response.put("media", media);

        // Add image dimensions map
        Map<String, Map<String, Object>> dimensions = new HashMap<>();

        for (MediaInfoEntity item : media) {
            if (item.getBanner() != null) {
                String bannerId = extractImageId(item.getBanner());
                int[] dims = IMAGE_DIMENSIONS.getOrDefault(bannerId, new int[]{1920, 400});

                Map<String, Object> itemDims = new HashMap<>();
                itemDims.put("width", dims[0]);
                itemDims.put("height", dims[1]);

                dimensions.put(String.valueOf(item.getId()), itemDims);
            }
        }

        response.put("dimensions", dimensions);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.MINUTES))
                .body(response);
    }

    private String extractImageId(String imageUrl) {
        if (imageUrl == null) return "";

        Matcher matcher = BANNER_PATTERN.matcher(imageUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

}
