package com.Toine.animeCountdownBackend.models.graphQlMedia;


import java.time.Instant;


public class NextAiringEpisode {
    private int episode;
    private Long airingAt;

    public int getEpisode() {
        return episode;
    }

    public Long getAiringAt() {
        return airingAt;
    }
}
