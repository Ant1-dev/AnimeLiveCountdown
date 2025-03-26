package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Column(name = "send_date")
    private Instant sendDate;

    // Fix relationship - assuming notification belongs to a user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getSendDate() {
        return sendDate;
    }

    public void setSendDate(Instant sendDate) {
        this.sendDate = sendDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}