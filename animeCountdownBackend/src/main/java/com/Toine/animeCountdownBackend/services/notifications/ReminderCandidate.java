package com.Toine.animeCountdownBackend.services.notifications;

import java.time.Instant;

/** A reminder of a followed show joined with that show's next episode. */
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
        String coverImageUrl) {

    public String displayTitle() {
        if (titleEnglish != null && !titleEnglish.isBlank()) {
            return titleEnglish;
        }
        return titleRomaji != null ? titleRomaji : "Unknown title";
    }
}
