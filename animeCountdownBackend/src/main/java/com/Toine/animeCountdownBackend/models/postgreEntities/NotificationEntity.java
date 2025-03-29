package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Instant notifDate;
    private String message;

    @ManyToOne( fetch = FetchType.LAZY)
    @JoinColumn( name = "user_id")
    private UserEntity user;

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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}