package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/fav-media")
public class FavoriteMediaController {
    private FavoriteMediaRepository favRepository;

    public FavoriteMediaController(FavoriteMediaRepository favRepository) {
        this.favRepository = favRepository;
    }

    @GetMapping("/{userId}")
    public List<MediaEntity> getFavoriteMedia(@PathVariable Long userId) {
        return favRepository.findFavMedia(userId);
    }
}
