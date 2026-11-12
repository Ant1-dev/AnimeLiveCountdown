package com.Toine.animeCountdownBackend.services;

import java.time.Instant;
import java.util.Optional;

/**
 * Works out the most recent episode of a show that has already aired, from what the schedule sync
 * stores. Used to decide which episodes can be reacted to.
 */
public final class LatestEpisode {

    private LatestEpisode() {
    }

    /**
     * @param status        AniList status (RELEASING, FINISHED, NOT_YET_RELEASED, HIATUS, CANCELLED)
     * @param nextEpisode   the next episode number the schedule knows about, if any
     * @param nextAiringAt  when that next episode airs, if known
     * @param totalEpisodes the planned episode count, if known
     * @param now           the current time
     */
    public static Optional<Integer> latestAired(String status, Integer nextEpisode, Instant nextAiringAt,
                                                Integer totalEpisodes, Instant now) {
        if ("NOT_YET_RELEASED".equals(status)) {
            return positive(nextEpisode != null && nextAiringAt != null && !nextAiringAt.isAfter(now)
                    ? nextEpisode : null);
        }
        if ("FINISHED".equals(status) && totalEpisodes != null && totalEpisodes > 0) {
            return Optional.of(totalEpisodes);
        }
        if (nextEpisode != null) {
            // The stored "next" episode may already have aired if the sync hasn't caught up yet
            boolean nextHasAired = nextAiringAt != null && !nextAiringAt.isAfter(now);
            return positive(nextHasAired ? nextEpisode : nextEpisode - 1);
        }
        if (("FINISHED".equals(status) || "CANCELLED".equals(status)) && totalEpisodes != null) {
            return positive(totalEpisodes);
        }
        return Optional.empty();
    }

    private static Optional<Integer> positive(Integer episode) {
        return episode != null && episode > 0 ? Optional.of(episode) : Optional.empty();
    }
}
