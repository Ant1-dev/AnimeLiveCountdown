package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/** Per-user notification channel settings: Discord webhook and calendar feed token. */
@Entity
@Table(name = "notification_settings",
       uniqueConstraints = @UniqueConstraint(name = "uk_notification_settings_calendar_token", columnNames = "calendar_token"))
public class NotificationSettingsEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "discord_webhook_url", length = 512)
    private String discordWebhookUrl;

    /** Secret that authenticates the public calendar feed URL; null when the feed is off. */
    @Column(name = "calendar_token", length = 64)
    private String calendarToken;

    @Column(name = "calendar_token_created_at")
    private Instant calendarTokenCreatedAt;

    public Long getUserId() {
        return userId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public String getCalendarToken() {
        return calendarToken;
    }

    public void setCalendarToken(String calendarToken) {
        this.calendarToken = calendarToken;
    }

    public Instant getCalendarTokenCreatedAt() {
        return calendarTokenCreatedAt;
    }

    public void setCalendarTokenCreatedAt(Instant calendarTokenCreatedAt) {
        this.calendarTokenCreatedAt = calendarTokenCreatedAt;
    }
}
