package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A user's reminder preference for one show. Reminders only fire while the show is in the user's
 * favorites; the row is kept when the show is unfollowed so following it again restores it.
 */
@Entity
@Table(name = "reminder",
       uniqueConstraints = @UniqueConstraint(name = "uk_reminder_user_media", columnNames = {"user_id", "media_id"}),
       indexes = @Index(name = "idx_reminder_media", columnList = "media_id"))
public class ReminderEntity {
    /** Longest lead time a reminder may use, in minutes (one day). */
    public static final int MAX_LEAD_MINUTES = 1440;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    // No foreign key: the schedule sync deletes media rows when shows stop airing
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    /** Minutes before airing to notify; 0 means at airing time. */
    @Column(name = "lead_minutes", nullable = false)
    private int leadMinutes;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "discord_enabled", nullable = false)
    private boolean discordEnabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public int getLeadMinutes() {
        return leadMinutes;
    }

    public void setLeadMinutes(int leadMinutes) {
        this.leadMinutes = leadMinutes;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public void setDiscordEnabled(boolean discordEnabled) {
        this.discordEnabled = discordEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
