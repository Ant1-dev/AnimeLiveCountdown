package com.Toine.animeCountdownBackend.models.postgreEntities;


import jakarta.persistence.Column;
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
    @Column(name = "next_airing_at")
    private Instant next_Airing_At;
    @Column(name = "next_airing_episode")
    private Integer next_Airing_Episode;
    @Column(name = "cover_image_url")
    private String cover_Image_Url;
    private String day;
    private Long popularity;

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

    public void setNext_Airing_At(Instant next_Airing_At) {
        this.next_Airing_At = next_Airing_At;
    }

    public void setNext_Airing_Episode(Integer nextAiringEpisode) {
        this.next_Airing_Episode = nextAiringEpisode;
    }


    public void setCover_Image_Url(String coverImageUrl) {
        this.cover_Image_Url = coverImageUrl;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public Long getId() {
        return id;
    }

    public String getTitle_Romaji() {
        return title_Romaji;
    }

    public String getTitle_English() {
        return title_English;
    }

    public String getStatus() {
        return status;
    }

    public Instant getNext_Airing_At() {
        return next_Airing_At;
    }

    public Integer getNext_Airing_Episode() {
        return next_Airing_Episode;
    }

    public String getCover_Image_Url() {
        return cover_Image_Url;
    }

    public String getDay() {
        return day;
    }

    public Long getPopularity() {
        return popularity;
    }

    public void setPopularity(Long popularity) {
        this.popularity = popularity;
    }
}
