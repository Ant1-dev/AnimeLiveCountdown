package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.time.Instant;

/** AnimeSchedule show (route) -> AniList id, looked up once and kept. aniListId is null when it has none. */
@Entity
@Table(name = "animeschedule_link")
public class AnimeScheduleLinkEntity {
    @Id
    @Column(length = 255)
    private String route;

    @Column(name = "anilist_id")
    private Long aniListId;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    public AnimeScheduleLinkEntity() {
    }

    public AnimeScheduleLinkEntity(String route, Long aniListId, Instant checkedAt) {
        this.route = route;
        this.aniListId = aniListId;
        this.checkedAt = checkedAt;
    }

    public String getRoute() { return route; }
    public Long getAniListId() { return aniListId; }
    public Instant getCheckedAt() { return checkedAt; }
}
