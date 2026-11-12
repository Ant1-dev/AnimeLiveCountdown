package com.Toine.animeCountdownBackend.models.postgreEntities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_day", columnList = "day"),
    @Index(name = "idx_media_next_airing_at", columnList = "next_airing_at"),
    @Index(name = "idx_media_status", columnList = "status"),
    @Index(name = "idx_media_popularity", columnList = "popularity"),
    @Index(name = "idx_media_season_year", columnList = "season_year"),
    @Index(name = "idx_media_season", columnList = "season"),
    // Composite indexes for common query patterns
    @Index(name = "idx_media_status_day", columnList = "status, day"),
    @Index(name = "idx_media_status_next_airing", columnList = "status, next_airing_at"),
    @Index(name = "idx_media_day_popularity", columnList = "day, popularity"),
    @Index(name = "idx_media_season_year_season", columnList = "season_year, season")
})
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
    // Weekday of next_Airing_At in UTC. Not used for the weekly schedule, which groups by the
    // viewer's timezone (see ScheduleController / AiringDayWindows).
    private String day;
    private Long popularity;
    private Integer seasonYear;
    private String season;
    @Column(name = "banner_image_url")
    private String banner_Image_Url;
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FavoriteMediaEntity> favorites;

    // Break / delay detection (see ScheduleNoteDetector). Exposed to clients only as scheduleNote.
    @Column(name = "schedule_note")
    private String scheduleNoteType;
    @Column(name = "schedule_note_detected_at")
    private Instant scheduleNoteDetectedAt;
    @Column(name = "schedule_note_original_at")
    private Instant scheduleNoteOriginalAt;
    @Column(name = "schedule_note_expires_at")
    private Instant scheduleNoteExpiresAt;
    // When the most recent episode aired, so a break can be measured once AniList adds the next date.
    @Column(name = "last_aired_at")
    private Instant lastAiredAt;

    public Integer getSeasonYear() {
        return seasonYear;
    }

    public void setSeasonYear(Integer seasonYear) {
        this.seasonYear = seasonYear;
    }

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

    @JsonIgnore
    public List<FavoriteMediaEntity> getFavorites() {
        return favorites;
    }

    @JsonIgnore
    public void setFavorites(List<FavoriteMediaEntity> favorites) {
        this.favorites = favorites;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getBanner_Image_Url() {
        return banner_Image_Url;
    }

    public void setBanner_Image_Url(String banner_Image_Url) {
        this.banner_Image_Url = banner_Image_Url;
    }

    /** The active break/delay note for clients, or null when there is none or it has expired. */
    @JsonProperty("scheduleNote")
    public ScheduleNote getScheduleNote() {
        return ScheduleNote.activeOf(this, Instant.now());
    }

    @JsonIgnore
    public ScheduleNote.Type getScheduleNoteType() {
        return scheduleNoteType == null ? null : ScheduleNote.Type.valueOf(scheduleNoteType);
    }

    public void setScheduleNoteType(ScheduleNote.Type type) {
        this.scheduleNoteType = type == null ? null : type.name();
    }

    @JsonIgnore
    public Instant getScheduleNoteDetectedAt() {
        return scheduleNoteDetectedAt;
    }

    public void setScheduleNoteDetectedAt(Instant scheduleNoteDetectedAt) {
        this.scheduleNoteDetectedAt = scheduleNoteDetectedAt;
    }

    @JsonIgnore
    public Instant getScheduleNoteOriginalAt() {
        return scheduleNoteOriginalAt;
    }

    public void setScheduleNoteOriginalAt(Instant scheduleNoteOriginalAt) {
        this.scheduleNoteOriginalAt = scheduleNoteOriginalAt;
    }

    @JsonIgnore
    public Instant getScheduleNoteExpiresAt() {
        return scheduleNoteExpiresAt;
    }

    public void setScheduleNoteExpiresAt(Instant scheduleNoteExpiresAt) {
        this.scheduleNoteExpiresAt = scheduleNoteExpiresAt;
    }

    @JsonIgnore
    public Instant getLastAiredAt() {
        return lastAiredAt;
    }

    public void setLastAiredAt(Instant lastAiredAt) {
        this.lastAiredAt = lastAiredAt;
    }
}
