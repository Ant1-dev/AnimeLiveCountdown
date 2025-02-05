package com.Toine.animeCountdownBackend.model;

import lombok.Getter;
import lombok.Setter;

public class Media {
    @Getter @Setter
    private Long id;
    @Getter @Setter
    private Title title;
    @Getter @Setter
    private String status;
    @Getter @Setter
    private NextAiringEpisode nextAiringEpisode;
    @Getter @Setter
    private CoverImage coverImage;

}
