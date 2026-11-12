package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.time.Instant;

/** When an episode releases in a given form (Japanese broadcast, subbed stream, English dub). */
@Entity
@Table(name = "release_time",
        uniqueConstraints = @UniqueConstraint(columnNames = {"media_id", "air_type", "episode"}),
        indexes = @Index(name = "idx_release_time_media", columnList = "media_id, air_type, release_at"))
public class ReleaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** AniList id. */
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    /** RAW, SUB or DUB. */
    @Column(name = "air_type", nullable = false, length = 8)
    private String airType;

    @Column(nullable = false)
    private Integer episode;

    @Column(name = "release_at", nullable = false)
    private Instant releaseAt;

    /** Streaming platforms as JSON: {"crunchyroll":"https://..."}. */
    @Column(name = "streams", columnDefinition = "TEXT")
    private String streamsJson;

    @Column(name = "delayed_until")
    private Instant delayedUntil;

    @Column(name = "delay_note", length = 255)
    private String delayNote;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getAirType() { return airType; }
    public void setAirType(String airType) { this.airType = airType; }
    public Integer getEpisode() { return episode; }
    public void setEpisode(Integer episode) { this.episode = episode; }
    public Instant getReleaseAt() { return releaseAt; }
    public void setReleaseAt(Instant releaseAt) { this.releaseAt = releaseAt; }
    public String getStreamsJson() { return streamsJson; }
    public void setStreamsJson(String streamsJson) { this.streamsJson = streamsJson; }
    public Instant getDelayedUntil() { return delayedUntil; }
    public void setDelayedUntil(Instant delayedUntil) { this.delayedUntil = delayedUntil; }
    public String getDelayNote() { return delayNote; }
    public void setDelayNote(String delayNote) { this.delayNote = delayNote; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
