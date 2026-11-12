package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.MediaListEntry;

import java.util.Optional;

/**
 * Decides what (if anything) to write to a user's AniList list when something changes here.
 * Conservative on purpose: it never lowers progress, never removes entries, and doesn't touch shows
 * the user already has on AniList unless their progress moved forward.
 */
public final class AniListSyncPlanner {
    private AniListSyncPlanner() {
    }

    /** What to save: an AniList MediaListStatus and, optionally, progress. */
    public record Save(String status, Integer progress) {
    }

    /** Followed here: add it to Planning, unless it's already on the AniList list in any state. */
    public static Optional<Save> onFollow(MediaListEntry existing) {
        return existing == null ? Optional.of(new Save("PLANNING", null)) : Optional.empty();
    }

    /** Marked episodes watched here: move AniList forward to match (Watching, or Completed at the end). */
    public static Optional<Save> onProgress(MediaListEntry existing, int watched, Integer totalEpisodes) {
        if (watched <= 0) {
            return Optional.empty();
        }
        int current = existing == null || existing.progress() == null ? 0 : existing.progress();
        if (existing != null && watched <= current) {
            return Optional.empty(); // AniList is already at or past this episode
        }
        boolean rewatching = existing != null && "REPEATING".equals(existing.status());
        boolean finished = totalEpisodes != null && totalEpisodes > 0 && watched >= totalEpisodes;
        String status = rewatching ? "REPEATING" : finished ? "COMPLETED" : "CURRENT";
        return Optional.of(new Save(status, watched));
    }
}
