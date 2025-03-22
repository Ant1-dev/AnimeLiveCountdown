package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MediaInfoController {

    private final MediaInfoRepository mediaInfoRepository;

    public MediaInfoController (MediaInfoRepository mediaInfoRepository) {
        this.mediaInfoRepository = mediaInfoRepository;
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<MediaInfoEntity> getMediaInfoById(@PathVariable Long id) {
        return mediaInfoRepository.findByIdWithGenres(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
