package com.Toine.animeCountdownBackend.model;

import lombok.Getter;
import lombok.Setter;

public class NextAiringEpisode {
    @Getter @Setter
    private int episode;
    @Getter @Setter
    private long AiringAt;

}
