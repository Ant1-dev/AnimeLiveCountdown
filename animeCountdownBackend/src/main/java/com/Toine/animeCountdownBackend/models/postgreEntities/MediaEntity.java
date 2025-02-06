package com.Toine.animeCountdownBackend.models.postgreEntities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "media")
public class MediaEntity {
    @Id
    private Long id;

    private String title_Romaji;
    private String title_English;
    private String status;
    private Instant next_Airing_At;
    private Integer next_Airing_Episode;
    private String cover_Image_Url;

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle_Romaji(String titleRomaji) {
        this.title_Romaji = titleRomaji;
    }

    public void setTitle_English(String titleEnglish) {
        this.title_English = titleEnglish;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNext_Airing_At(Instant nextAiringAt) {
        this.next_Airing_At = nextAiringAt;
    }

    public void setNext_Airing_Episode(Integer nextAiringEpisode) {
        this.next_Airing_Episode = nextAiringEpisode;
    }


    public void setCover_Image_Url(String coverImageUrl) {
        this.cover_Image_Url = coverImageUrl;
    }
}
