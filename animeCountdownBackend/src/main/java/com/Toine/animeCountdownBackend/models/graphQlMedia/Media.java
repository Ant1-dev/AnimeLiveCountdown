package com.Toine.animeCountdownBackend.models.graphQlMedia;

public class Media {
    private Long id;
    private Title title;
    private String status;
    private NextAiringEpisode nextAiringEpisode;
    private CoverImage coverImage;

    public Long getId() {
        return id;
    }

    public Title getTitle() {
        return title;
    }

    public void setNextAiringEpisode(NextAiringEpisode nextAiringEpisode) {
        this.nextAiringEpisode = nextAiringEpisode;
    }

    public void setCoverImage(CoverImage coverImage) {
        this.coverImage = coverImage;
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
