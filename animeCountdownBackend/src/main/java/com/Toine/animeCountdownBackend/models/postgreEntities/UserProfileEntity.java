package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;

@Entity
public class UserProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Add profile fields as needed
    private String profilePictureUrl;

    @OneToOne(mappedBy = "userProfile", optional = false)
    private User user;

    // Add getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}