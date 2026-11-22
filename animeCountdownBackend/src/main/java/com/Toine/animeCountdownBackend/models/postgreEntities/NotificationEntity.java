package com.Toine.animeCountdownBackend.models.postgreEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * An entry in a user's notification bell.
 *
 * Episode reminders and delay alerts also use this row as the "already sent" record: the unique key
 * on (user_id, media_id, episode, kind) means each user hears about each episode once per kind
 * (Japanese airing, sub, dub, delay), however many times the jobs run (see ReminderDispatchService
 * and DelayAlertService). The key used to be (user_id, media_id, episode); SchemaMigrations
 * replaces it on existing databases.
 */
@Entity
@Table(name = "notification_entity",
       uniqueConstraints = @UniqueConstraint(name = NotificationEntity.UNIQUE_KEY,
                                             columnNames = {"user_id", "media_id", "episode", "kind"}),
       indexes = @Index(name = "idx_notification_user_date", columnList = "user_id, notif_date"))
public class NotificationEntity {
    public static final String UNIQUE_KEY = "uk_notification_user_media_episode_kind";

    /** Reminder for the Japanese broadcast. */
    public static final String KIND_EPISODE = "EPISODE";
    /** Reminder for the subtitled stream. */
    public static final String KIND_EPISODE_SUB = "EPISODE_SUB";
    /** Reminder for the English dub. */
    public static final String KIND_EPISODE_DUB = "EPISODE_DUB";
    /** The next episode was delayed or the show went on break (Japanese airing / sub). */
    public static final String KIND_DELAY = "DELAY";
    /** The next dubbed episode was delayed. */
    public static final String KIND_DELAY_DUB = "DELAY_DUB";

    /** Notification kind for an episode reminder of the given reminder release type. */
    public static String episodeKind(String releaseType) {
        return switch (releaseType == null ? "" : releaseType) {
            case ReminderEntity.TYPE_SUB -> KIND_EPISODE_SUB;
            case ReminderEntity.TYPE_DUB -> KIND_EPISODE_DUB;
            default -> KIND_EPISODE;
        };
    }

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

    /** Where to watch (a streaming platform URL), when known. */
    @Column(name = "link", length = 1024)
    private String link;

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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
