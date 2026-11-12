package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.ListEntry;
import com.Toine.animeCountdownBackend.anilist.AniListModels.ListMedia;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Decides what an AniList import changes, without touching the database.
 *
 * Rules:
 * - Every CURRENT / PLANNING entry becomes a follow, with watched-up-to = the entry's progress.
 * - Shows missing from the media table are added from the AniList data if they are RELEASING or
 *   NOT_YET_RELEASED (the sync jobs keep those fresh and never delete followed rows). Missing shows in
 *   any other status (FINISHED, CANCELLED, HIATUS) are skipped: they have no countdown or info page here.
 * - Adult shows are skipped; the site never lists them.
 * - Existing follows are kept and their progress only goes up (max of local and AniList), so a
 *   re-sync never undoes episodes marked watched on this site and never unfollows anything.
 */
public final class AniListImportPlanner {
    static final Set<String> INSERTABLE_STATUSES = Set.of("RELEASING", "NOT_YET_RELEASED");

    private AniListImportPlanner() {
    }

    public record Plan(
            List<MediaEntity> mediaToInsert,
            Map<Long, Integer> newFollows,
            Map<Long, Integer> progressRaises,
            int unchanged,
            int skippedNotAiring,
            int skippedAdult) {

        public int entriesConsidered() {
            return newFollows.size() + progressRaises.size() + unchanged + skippedNotAiring + skippedAdult;
        }
    }

    /**
     * @param entries          the user's AniList CURRENT + PLANNING entries
     * @param mediaInDb        IDs of those shows already in the media table
     * @param followedProgress the user's existing follows among those shows: media ID -> watched episode
     */
    public static Plan plan(List<ListEntry> entries, Set<Long> mediaInDb, Map<Long, Integer> followedProgress) {
        // A show is on one AniList list at a time, but be safe: keep the highest progress per show
        Map<Long, ListEntry> byMedia = new LinkedHashMap<>();
        for (ListEntry entry : entries) {
            if (entry == null || entry.media() == null || entry.media().id() == null) {
                continue;
            }
            byMedia.merge(entry.media().id(), entry,
                    (a, b) -> progressOf(b) > progressOf(a) ? b : a);
        }

        List<MediaEntity> toInsert = new ArrayList<>();
        Map<Long, Integer> newFollows = new LinkedHashMap<>();
        Map<Long, Integer> raises = new LinkedHashMap<>();
        int unchanged = 0;
        int skippedNotAiring = 0;
        int skippedAdult = 0;

        for (ListEntry entry : byMedia.values()) {
            ListMedia media = entry.media();
            long id = media.id();
            int progress = progressOf(entry);

            if (Boolean.TRUE.equals(media.adult())) {
                skippedAdult++;
            } else if (followedProgress.containsKey(id)) {
                if (progress > followedProgress.get(id)) {
                    raises.put(id, progress);
                } else {
                    unchanged++;
                }
            } else if (mediaInDb.contains(id)) {
                newFollows.put(id, progress);
            } else if (media.status() != null && INSERTABLE_STATUSES.contains(media.status())) {
                toInsert.add(toMediaEntity(media));
                newFollows.put(id, progress);
            } else {
                skippedNotAiring++;
            }
        }
        return new Plan(toInsert, newFollows, raises, unchanged, skippedNotAiring, skippedAdult);
    }

    private static int progressOf(ListEntry entry) {
        return entry.progress() == null ? 0 : Math.max(0, entry.progress());
    }

    /** Same mapping as the schedule sync (ScheduleService.convertToEntity). */
    static MediaEntity toMediaEntity(ListMedia media) {
        MediaEntity entity = new MediaEntity();
        entity.setId(media.id());
        if (media.title() != null) {
            entity.setTitle_Romaji(media.title().romaji());
            entity.setTitle_English(media.title().english());
        }
        entity.setStatus(media.status());
        entity.setPopularity(media.popularity());
        entity.setSeasonYear(media.seasonYear());
        entity.setSeason(media.season());
        entity.setBanner_Image_Url(media.bannerImage());
        if (media.nextAiringEpisode() != null && media.nextAiringEpisode().airingAt() != null) {
            Instant airingAt = Instant.ofEpochSecond(media.nextAiringEpisode().airingAt());
            entity.setNext_Airing_At(airingAt);
            entity.setNext_Airing_Episode(media.nextAiringEpisode().episode());
            entity.setDay(airingAt.atOffset(ZoneOffset.UTC).getDayOfWeek().toString());
        }
        if (media.coverImage() != null) {
            entity.setCover_Image_Url(media.coverImage().large());
        }
        return entity;
    }
}
