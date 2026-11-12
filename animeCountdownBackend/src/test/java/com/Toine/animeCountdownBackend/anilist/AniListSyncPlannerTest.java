package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.MediaListEntry;
import com.Toine.animeCountdownBackend.anilist.AniListSyncPlanner.Save;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AniListSyncPlannerTest {

    private static MediaListEntry entry(String status, Integer progress) {
        return new MediaListEntry(1L, status, progress);
    }

    @Test
    void followAddsToPlanningOnlyWhenNotOnTheList() {
        assertEquals(Optional.of(new Save("PLANNING", null)), AniListSyncPlanner.onFollow(null));
        assertTrue(AniListSyncPlanner.onFollow(entry("CURRENT", 3)).isEmpty());
        assertTrue(AniListSyncPlanner.onFollow(entry("COMPLETED", 12)).isEmpty());
        assertTrue(AniListSyncPlanner.onFollow(entry("DROPPED", 2)).isEmpty());
    }

    @Test
    void progressMovesAniListForwardAsWatching() {
        assertEquals(Optional.of(new Save("CURRENT", 4)), AniListSyncPlanner.onProgress(entry("PLANNING", 0), 4, 12));
        assertEquals(Optional.of(new Save("CURRENT", 1)), AniListSyncPlanner.onProgress(null, 1, 12));
        assertEquals(Optional.of(new Save("CURRENT", 5)), AniListSyncPlanner.onProgress(entry("PAUSED", 3), 5, null));
    }

    @Test
    void progressNeverLowersOrRepeatsAniList() {
        assertTrue(AniListSyncPlanner.onProgress(entry("CURRENT", 6), 4, 12).isEmpty());
        assertTrue(AniListSyncPlanner.onProgress(entry("CURRENT", 6), 6, 12).isEmpty());
        assertTrue(AniListSyncPlanner.onProgress(entry("COMPLETED", 12), 12, 12).isEmpty());
        assertTrue(AniListSyncPlanner.onProgress(null, 0, 12).isEmpty());
    }

    @Test
    void lastEpisodeCompletesAndRewatchesStayRewatches() {
        assertEquals(Optional.of(new Save("COMPLETED", 12)), AniListSyncPlanner.onProgress(entry("CURRENT", 11), 12, 12));
        assertEquals(Optional.of(new Save("REPEATING", 12)), AniListSyncPlanner.onProgress(entry("REPEATING", 5), 12, 12));
    }
}
