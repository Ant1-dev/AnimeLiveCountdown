package com.Toine.animeCountdownBackend.models.postgreEntities;

import com.Toine.animeCountdownBackend.models.ReactionType;
import jakarta.persistence.*;

import java.time.Instant;

/** One user's reaction to one episode of a show. A user has at most one reaction per episode. */
@Entity
@Table(name = "episode_reaction",
       uniqueConstraints = @UniqueConstraint(name = "uk_episode_reaction_user_media_episode",
               columnNames = {"user_id", "media_id", "episode"}),
       indexes = {
           @Index(name = "idx_episode_reaction_media_episode", columnList = "media_id, episode"),
           @Index(name = "idx_episode_reaction_updated_at", columnList = "updated_at")
       })
public class EpisodeReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // AniList id. Deliberately not a foreign key to media, so the schedule sync never trips over it.
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "episode", nullable = false)
    private Integer episode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, length = 16)
    private ReactionType reaction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getEpisode() {
        return episode;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
    }

    public ReactionType getReaction() {
        return reaction;
    }

    public void setReaction(ReactionType reaction) {
        this.reaction = reaction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
