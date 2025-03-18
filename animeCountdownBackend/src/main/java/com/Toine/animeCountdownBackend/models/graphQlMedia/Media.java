package com.Toine.animeCountdownBackend.models.graphQlMedia;

public class Media {
    private Long id;
    private Title title;
    private String status;
    private NextAiringEpisode nextAiringEpisode;
    private CoverImage coverImage;
    private Long popularity;

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

}
