package com.Toine.animeCountdownBackend.models.postgreEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * An entry in a user's notification bell.
 *
 * Episode reminders also use this row as the "already sent" record: the unique key on
 * (user_id, media_id, episode) means each user is notified about each episode once, however many
 * times the reminder job runs (see ReminderDispatchService).
 */
@Entity
@Table(name = "notification_entity",
       uniqueConstraints = @UniqueConstraint(name = "uk_notification_user_media_episode",
                                             columnNames = {"user_id", "media_id", "episode"}),
       indexes = @Index(name = "idx_notification_user_date", columnList = "user_id, notif_date"))
public class NotificationEntity {
    public static final String KIND_EPISODE = "EPISODE";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Instant notifDate;
    private String message;

    @ManyToOne( fetch = FetchType.LAZY)
    @JoinColumn( name = "user_id")
    @JsonIgnore
    private UserEntity user;

    // No foreign key: the schedule sync deletes media rows when shows stop airing
    @Column(name = "media_id")
    private Long mediaId;

    private Integer episode;

    @Column(name = "airing_at")
    private Instant airingAt;

    private String title;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    private String kind;

    @Column(name = "read_at")
    private Instant readAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getNotifDate() {
        return notifDate;
    }

    public void setNotifDate(Instant notifDate) {
        this.notifDate = notifDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonIgnore
    public UserEntity getUser() {
        return user;
    }

    @JsonIgnore
    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Integer getEpisode() {
        return episode;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
    }

    public Instant getAiringAt() {
        return airingAt;
    }

    public void setAiringAt(Instant airingAt) {
        this.airingAt = airingAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
