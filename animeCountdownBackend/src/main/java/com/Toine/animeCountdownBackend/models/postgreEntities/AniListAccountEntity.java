package com.Toine.animeCountdownBackend.models.postgreEntities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * An AniList account linked to a user, used to import their anime list. One per user.
 * The access token is stored encrypted (see AniListTokenCipher) and never serialized.
 */
@Entity
@Table(name = "anilist_account")
public class AniListAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "anilist_user_id", nullable = false)
    private Long aniListUserId;

    @Column(name = "anilist_user_name")
    private String aniListUserName;

    @Column(name = "access_token", nullable = false, length = 4096)
    private String encryptedAccessToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "connected_at")
    private Instant connectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /** Push follows and watched episodes to the user's AniList list. Null (existing rows) means on. */
    @Column(name = "sync_enabled")
    private Boolean syncEnabled;

    public boolean isSyncEnabled() {
        return syncEnabled == null || syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getAniListUserId() {
        return aniListUserId;
    }

    public void setAniListUserId(Long aniListUserId) {
        this.aniListUserId = aniListUserId;
    }

    public String getAniListUserName() {
        return aniListUserName;
    }

    public void setAniListUserName(String aniListUserName) {
        this.aniListUserName = aniListUserName;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
