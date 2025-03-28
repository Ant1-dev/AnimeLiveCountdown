package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class UserProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String profilePictureUrl;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationEntity> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "userProfile")
    private UserEntity user;

}