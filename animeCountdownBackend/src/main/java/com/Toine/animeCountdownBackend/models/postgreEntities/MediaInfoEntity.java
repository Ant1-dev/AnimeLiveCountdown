package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "media_info")
public class MediaInfoEntity {
    @Id
    private Long id;

    @Column(name = "eng_title")
    private String engtitle;

    @Column(name = "rom_title")
    private String romtitle;

    @Column(name = "next_episode")
    private Integer nextepisode;

    @Column(name = "total_episodes")
    private Integer totalepisodes;

    @Column(name = "media_status")
    private String status;

    @Column(name = "media_popularity")
    private Long popularity;

    @Column(name = "episode_duration")
    private Integer duration;

    @ElementCollection
    @CollectionTable(
            name = "media_info_genres",
            joinColumns = @JoinColumn(name = "media_info_id")
    )

    @Column(name = "genre")
    private List<String> genres;

    @Column(name = "avg_score")
    private Integer avgscore;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image")
    private String coverimage;

    @Column(name = "banner_image")
    private String banner;

    @Column(name = "airing_at")
    private Instant airingat;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEngtitle() {
        return engtitle;
    }

    public void setEngtitle(String engtitle) {
        this.engtitle = engtitle;
    }

    public String getRomtitle() {
        return romtitle;
    }

    public void setRomtitle(String romtitle) {
        this.romtitle = romtitle;
    }

    public Integer getNextepisode() {
        return nextepisode;
    }

    public void setNextepisode(Integer nextepisode) {
        this.nextepisode = nextepisode;
    }

    public Integer getTotalepisodes() {
        return totalepisodes;
    }

    public void setTotalepisodes(Integer totalepisodes) {
        this.totalepisodes = totalepisodes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPopularity() {
        return popularity;
    }

    public void setPopularity(Long popularity) {
        this.popularity = popularity;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Integer getAvgscore() {
        return avgscore;
    }

    public void setAvgscore(Integer avgscore) {
        this.avgscore = avgscore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverimage() {
        return coverimage;
    }

    public void setCoverimage(String coverimage) {
        this.coverimage = coverimage;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public Instant getAiringat() {
        return airingat;
    }

    public void setAiringat(Instant airingat) {
        this.airingat = airingat;
    }
}
