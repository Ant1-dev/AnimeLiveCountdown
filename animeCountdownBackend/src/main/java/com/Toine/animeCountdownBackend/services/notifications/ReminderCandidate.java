package com.Toine.animeCountdownBackend.services.notifications;

import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;

import java.time.Instant;

/**
 * A reminder of a followed show joined with the release it is for: the show's next Japanese airing
 * (release type JAPAN) or a subbed/dubbed release from release_time (SUB/DUB, with its streams).
 */
public record ReminderCandidate(
        Long userId,
        Long mediaId,
        int leadMinutes,
        boolean pushEnabled,
        boolean discordEnabled,
        Instant airingAt,
        Integer episode,
        String titleEnglish,
        String titleRomaji,
        String coverImageUrl,
        String releaseType,
        String streamsJson) {

    /** A JAPAN reminder joined with AniList's next airing (ReminderRepository.findCandidates). */
    public ReminderCandidate(Long userId, Long mediaId, int leadMinutes, boolean pushEnabled, boolean discordEnabled,
                             Instant airingAt, Integer episode, String titleEnglish, String titleRomaji,
                             String coverImageUrl) {
        this(userId, mediaId, leadMinutes, pushEnabled, discordEnabled, airingAt, episode, titleEnglish, titleRomaji,
                coverImageUrl, ReminderEntity.TYPE_JAPAN, null);
    }

    /**
     * A SUB/DUB reminder joined with a release_time row (ReminderRepository.findReleaseCandidates).
     * A delayed release is out at delayed_until, not at its listed time.
     */
    public ReminderCandidate(Long userId, Long mediaId, int leadMinutes, boolean pushEnabled, boolean discordEnabled,
                             Instant releaseAt, Instant delayedUntil, Integer episode, String titleEnglish,
                             String titleRomaji, String coverImageUrl, String releaseType, String streamsJson) {
        this(userId, mediaId, leadMinutes, pushEnabled, discordEnabled,
                delayedUntil != null && delayedUntil.isAfter(releaseAt) ? delayedUntil : releaseAt,
                episode, titleEnglish, titleRomaji, coverImageUrl, releaseType, streamsJson);
    }

    public String displayTitle() {
        if (titleEnglish != null && !titleEnglish.isBlank()) {
            return titleEnglish;
        }
        return titleRomaji != null ? titleRomaji : "Unknown title";
    }
}
