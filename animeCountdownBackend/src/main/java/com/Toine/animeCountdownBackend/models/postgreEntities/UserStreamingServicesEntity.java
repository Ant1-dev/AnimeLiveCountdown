package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/** A user's "My services": picked streaming service keys and their own platforms, as JSON. */
@Entity
@Table(name = "user_streaming_services")
public class UserStreamingServicesEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    /** JSON array of StreamingServices keys, e.g. ["crunchyroll","netflix"]. */
    @Column(name = "services", columnDefinition = "TEXT")
    private String servicesJson;

    /** JSON array of {"name","url"} objects. */
    @Column(name = "custom_platforms", columnDefinition = "TEXT")
    private String customPlatformsJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getUserId() {
        return userId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getServicesJson() {
        return servicesJson;
    }

    public void setServicesJson(String servicesJson) {
        this.servicesJson = servicesJson;
    }

    public String getCustomPlatformsJson() {
        return customPlatformsJson;
    }

    public void setCustomPlatformsJson(String customPlatformsJson) {
        this.customPlatformsJson = customPlatformsJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
