package com.Toine.animeCountdownBackend.models.graphQlMedia;

public class Media {
    private Long id;
    private Title title;
    private String status;
    private NextAiringEpisode nextAiringEpisode;
    private CoverImage coverImage;
    private String bannerImage;
    private Long popularity;
    private Integer seasonYear;
    private String season;

    public Integer getSeasonYear() {
        return seasonYear;
    }

    public Long getId() {
        return id;
    }

    public Long getPopularity() { return popularity; }

    public Title getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public NextAiringEpisode getNextAiringEpisode() {
        return nextAiringEpisode;
    }

    public CoverImage getCoverImage() {
        return coverImage;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public String getSeason() {
        return season;
    }

}
