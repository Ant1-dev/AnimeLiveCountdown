package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A past season fetched from AniList on demand, stored as JSON so it survives restarts. */
@Entity
@Table(name = "season_cache")
public class SeasonCacheEntity {
    @Id
    @Column(name = "season_key", length = 16)
    private String seasonKey;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected SeasonCacheEntity() {
    }

    public SeasonCacheEntity(String seasonKey, String payload, Instant fetchedAt) {
        this.seasonKey = seasonKey;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public String getSeasonKey() {
        return seasonKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
