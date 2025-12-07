package com.Toine.animeCountdownBackend.models.postgreEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "favmedia",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "media_id"}),
       indexes = {
           @Index(name = "idx_favmedia_user_id", columnList = "user_id"),
           @Index(name = "idx_favmedia_media_id", columnList = "media_id")
       })
public class FavoriteMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    @JsonIgnore
    private MediaEntity media;

    @Column(name = "added_date")
    private Instant addedDate = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public UserEntity getUser() {
        return user;
    }

    @JsonIgnore
    public void setUser(UserEntity user) {
        this.user = user;
    }

    @JsonIgnore
    public MediaEntity getMedia() {
        return media;
    }

    @JsonIgnore
    public void setMedia(MediaEntity media) {
        this.media = media;
    }

    public Instant getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Instant addedDate) {
        this.addedDate = addedDate;
    }
}
