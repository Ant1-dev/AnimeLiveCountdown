package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;

import java.time.Instant;

/** Works out how many episodes of a show have aired, and how far behind a viewer is. */
public final class EpisodeProgress {
    private EpisodeProgress() {
    }

    /**
     * The latest episode that has aired, or null when it isn't known.
     *
     * @param totalEpisodes the show's episode count if known (used for finished shows), else null
     */
    public static Integer latestAired(MediaEntity media, Integer totalEpisodes, Instant now) {
        if (media == null) {
            return null;
        }
        Integer next = media.getNext_Airing_Episode();
        Instant nextAt = media.getNext_Airing_At();
        if (next != null && nextAt != null) {
            // The stored "next" episode may already have aired if the just-aired sync hasn't run yet
            int aired = nextAt.isAfter(now) ? next - 1 : next;
            return Math.max(0, aired);
        }
        if ("NOT_YET_RELEASED".equals(media.getStatus())) {
            return 0;
        }
        if ("FINISHED".equals(media.getStatus()) && totalEpisodes != null && totalEpisodes > 0) {
            return totalEpisodes;
        }
        return null;
    }

    /** Aired episodes the viewer hasn't watched, or null if the aired count isn't known. */
    public static Integer behind(Integer latestAired, int watchedEpisode) {
        if (latestAired == null) {
            return null;
        }
        return Math.max(0, latestAired - watchedEpisode);
    }
}
