package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "media_info")
public class MediaInfoEntity {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private MediaEntity mediaEntity;

    private String engtitle;
    private String romtitle;
    private Integer nextepisode;
    private Integer totalepisodes;
    private String status;
    private Long popularity;
    private Integer duration;
    @ElementCollection
    private List<String> genres;
    private Integer avgscore;
    private String description;
    private String coverimage;
    private String banner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MediaEntity getMediaEntity() {
        return mediaEntity;
    }

    public void setMediaEntity(MediaEntity mediaEntity) {
        this.mediaEntity = mediaEntity;
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
}
