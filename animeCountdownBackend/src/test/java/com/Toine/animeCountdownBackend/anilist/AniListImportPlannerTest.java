package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.ListEntry;
import com.Toine.animeCountdownBackend.anilist.AniListModels.ListMedia;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AniListImportPlannerTest {

    private static final Set<Long> IN_DB = Set.of(171018L, 160000L, 150000L);
    private static final Map<Long, Integer> FOLLOWED = Map.of(171018L, 3, 160000L, 4);

    @Test
    void parsesRecordedResponseIntoEntriesFromEveryList() {
        List<ListEntry> entries = AniListFixtures.listEntries();

        assertEquals(7, entries.size());
        ListEntry first = entries.getFirst();
        assertEquals("CURRENT", first.status());
        assertEquals(5, first.progress());
        assertEquals(171018L, first.media().id());
        assertFalse(first.media().adult());
        assertEquals(7, first.media().nextAiringEpisode().episode());
        assertTrue(entries.stream().anyMatch(e -> Boolean.TRUE.equals(e.media().adult())));
    }

    @Test
    void mapsEachEntryAccordingToTheImportRules() {
        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(AniListFixtures.listEntries(), IN_DB, FOLLOWED);

        // Missing airing/upcoming shows are added; missing finished shows and adult shows are skipped
        assertEquals(List.of(182255L, 190000L), plan.mediaToInsert().stream().map(MediaEntity::getId).toList());
        assertEquals(1, plan.skippedNotAiring());
        assertEquals(1, plan.skippedAdult());

        // New follows are seeded with AniList progress (PLANNING = 0)
        assertEquals(Map.of(182255L, 3, 190000L, 0, 150000L, 0), plan.newFollows());

        // Existing follows only move forward: 3 -> 5 is raised, local 4 beats AniList 1
        assertEquals(Map.of(171018L, 5), plan.progressRaises());
        assertEquals(1, plan.unchanged());
        assertEquals(7, plan.entriesConsidered());
    }

    @Test
    void insertedShowUsesTheSameFieldsAsTheScheduleSync() {
        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(AniListFixtures.listEntries(), IN_DB, FOLLOWED);

        MediaEntity added = plan.mediaToInsert().getFirst();
        assertEquals("Not In Our Table Yet", added.getTitle_Romaji());
        assertNull(added.getTitle_English());
        assertEquals("RELEASING", added.getStatus());
        assertEquals(Instant.ofEpochSecond(1795100000L), added.getNext_Airing_At());
        assertEquals(4, added.getNext_Airing_Episode());
        assertEquals("https://img.anili.st/cover/182255.jpg", added.getCover_Image_Url());
        assertEquals(41000L, added.getPopularity());
        assertEquals("FALL", added.getSeason());
        assertEquals(2026, added.getSeasonYear());
        assertNotNull(added.getDay());
    }

    @Test
    void finishedShowAlreadyInTableIsFollowedNotSkipped() {
        ListEntry finished = entry(21L, "FINISHED", 10, false);

        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(List.of(finished), Set.of(21L), Map.of());

        assertEquals(Map.of(21L, 10), plan.newFollows());
        assertTrue(plan.mediaToInsert().isEmpty());
        assertEquals(0, plan.skippedNotAiring());
    }

    @Test
    void ignoresBrokenEntriesAndNegativeOrMissingProgress() {
        ListEntry noMedia = new ListEntry("CURRENT", 3, null);
        ListEntry nullProgress = entry(5L, "RELEASING", null, false);
        ListEntry negative = entry(6L, "RELEASING", -2, false);

        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(
                java.util.Arrays.asList(noMedia, null, nullProgress, negative), Set.of(5L, 6L), Map.of());

        assertEquals(Map.of(5L, 0, 6L, 0), plan.newFollows());
    }

    @Test
    void reSyncWithNothingNewChangesNothing() {
        ListEntry e = entry(7L, "RELEASING", 2, false);

        AniListImportPlanner.Plan plan = AniListImportPlanner.plan(List.of(e), Set.of(7L), Map.of(7L, 2));

        assertTrue(plan.newFollows().isEmpty());
        assertTrue(plan.progressRaises().isEmpty());
        assertTrue(plan.mediaToInsert().isEmpty());
        assertEquals(1, plan.unchanged());
    }

    private static ListEntry entry(long id, String status, Integer progress, boolean adult) {
        ListMedia media = new ListMedia(id, adult, status, 12, 100L, 2026, "FALL", null,
                new AniListModels.Title("T", "T"), null, null);
        return new ListEntry("CURRENT", progress, media);
    }
}
