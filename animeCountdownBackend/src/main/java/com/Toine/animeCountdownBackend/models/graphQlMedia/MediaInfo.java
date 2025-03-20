package com.Toine.animeCountdownBackend.models.graphQlMedia;

import java.util.List;

public class MediaInfo {
    private Long id;
    private Title title;
    private NextAiringEpisode nextAiringEpisode;
    private String bannerImage;
    private CoverImage coverImage;
    private String status;
    private Long popularity;
    private Integer episodes;
    private Integer duration;
    private List<String> genres;

    public Title getTitle() {
        return title;
    }

    public NextAiringEpisode getNextAiringEpisode() {
        return nextAiringEpisode;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public CoverImage getCoverImage() {
        return coverImage;
    }

    public String getStatus() {
        return status;
    }

    public Long getPopularity() {
        return popularity;
    }

    public Integer getEpisodes() {
        return episodes;
    }

    public Integer getDuration() {
        return duration;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Integer getAverageScore() {
        return averageScore;
    }

    public String getDescription() {
        return description;
    }

    private Integer averageScore;
    private String description;

    public Long getId() {
        return id;
    }
}
