package com.Toine.animeCountdownBackend.models.graphQlMedia;


import java.time.Instant;


public class NextAiringEpisode {
    private int episode;
    private Long AiringAt;

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public void setAiringAt(Long airingAt) {
        AiringAt = airingAt;
    }

    public Long getAiringAt() {
        return AiringAt;
    }
}
