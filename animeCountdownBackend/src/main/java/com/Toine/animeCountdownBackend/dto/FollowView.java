package com.Toine.animeCountdownBackend.dto;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;

import java.time.Instant;

/**
 * A followed show plus the viewer's progress.
 *
 * @param latestAiredEpisode null when unknown (e.g. a finished show with no episode count)
 * @param behind             aired episodes not yet watched; null when latestAiredEpisode is unknown
 */
public record FollowView(
        MediaEntity media,
        int watchedEpisode,
        Integer latestAiredEpisode,
        Integer behind,
        Instant addedDate,
        Instant progressUpdatedAt) {
}
