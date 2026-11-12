package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ScheduleNote;

import java.time.Duration;
import java.time.Instant;

/**
 * Notices when a show goes on break or gets delayed, from the difference between the stored row and
 * fresh AniList data. Needs no extra AniList requests: whether the episode number advanced tells a
 * break (an episode aired, the next one is far out) from a delay (the same episode moved later).
 *
 * <p>Call {@link #detect} with the stored row before the fresh values are copied onto it. It only
 * touches the note and tracking fields.
 */
public final class ScheduleNoteDetector {

    /** A weekly show whose next episode is further out than this after the last one is on break. */
    static final Duration BREAK_GAP = Duration.ofDays(8);
    /** Smaller pushes are usually AniList correcting a time, not a real delay. */
    static final Duration MIN_DELAY = Duration.ofHours(1);
    /** Premiere dates are often placeholders that get refined, so only flag bigger moves. */
    static final Duration MIN_PREMIERE_DELAY = Duration.ofDays(1);

    private static final String NOT_YET_RELEASED = "NOT_YET_RELEASED";

    private ScheduleNoteDetector() {
    }

    public static void detect(MediaEntity stored, MediaEntity fresh, Instant now) {
        clearIfExpired(stored, now);

        Instant oldAt = stored.getNext_Airing_At();
        Integer oldEpisode = stored.getNext_Airing_Episode();
        Instant newAt = fresh.getNext_Airing_At();
        Integer newEpisode = fresh.getNext_Airing_Episode();

        if (newAt == null) {
            // The episode aired but AniList has no next date yet. Remember when it aired so the gap
            // can be measured once the next date shows up.
            if (oldAt != null && !oldAt.isAfter(now)) {
                stored.setLastAiredAt(oldAt);
            }
            return;
        }

        if (oldAt == null) {
            // The next date was just announced after a stretch with none.
            if (stored.getLastAiredAt() != null && !NOT_YET_RELEASED.equals(fresh.getStatus())) {
                onEpisodeAired(stored, stored.getLastAiredAt(), newAt, now);
            }
            return;
        }

        if (oldEpisode != null && newEpisode != null) {
            int episodesAired = newEpisode - oldEpisode;
            if (episodesAired == 1) {
                Instant airedAt = oldAt.isAfter(now) ? now : oldAt;
                stored.setLastAiredAt(airedAt);
                onEpisodeAired(stored, airedAt, newAt, now);
                return;
            }
            if (episodesAired > 1) {
                // Several episodes aired since the last sync (e.g. after downtime) or a multi-episode
                // drop. The airings in between weren't seen, so the gap says nothing about a break.
                Duration perEpisode = Duration.between(oldAt, newAt).dividedBy(episodesAired);
                Instant lastAired = newAt.minus(perEpisode);
                stored.setLastAiredAt(lastAired.isAfter(now) ? now : lastAired);
                clearNote(stored);
                return;
            }
            if (newEpisode < oldEpisode) {
                return; // AniList correcting its episode numbers; nothing to conclude
            }
        }

        if (newAt.isAfter(oldAt)) {
            onPushedBack(stored, fresh, oldAt, newAt, now);
        } else if (newAt.isBefore(oldAt)) {
            onPulledForward(stored, fresh, newAt);
        }
    }

    /** An episode aired at airedAt and the next one is scheduled for nextAt. */
    private static void onEpisodeAired(MediaEntity stored, Instant airedAt, Instant nextAt, Instant now) {
        // Assumes a weekly cadence. Replaying real AniList schedules showed back-to-back skipped weeks
        // (e.g. long-running shows around sports) but no bi-weekly shows, so a repeated long gap is
        // still treated as a break.
        Duration gap = Duration.between(airedAt, nextAt);
        if (gap.compareTo(BREAK_GAP) > 0) {
            setNote(stored, ScheduleNote.Type.ON_BREAK, now, airedAt, nextAt);
        } else {
            clearNote(stored);
        }
    }

    /** The same episode moved later. */
    private static void onPushedBack(MediaEntity stored, MediaEntity fresh, Instant oldAt, Instant newAt, Instant now) {
        ScheduleNote.Type current = stored.getScheduleNoteType();
        if (current == ScheduleNote.Type.ON_BREAK) {
            stored.setScheduleNoteExpiresAt(newAt); // the return date moved; still on break
            return;
        }
        boolean alreadyDelayed = current == ScheduleNote.Type.DELAYED;
        Instant original = alreadyDelayed ? stored.getScheduleNoteOriginalAt() : oldAt;
        if (Duration.between(original, newAt).compareTo(minDelay(fresh)) >= 0) {
            Instant detectedAt = alreadyDelayed ? stored.getScheduleNoteDetectedAt() : now;
            setNote(stored, ScheduleNote.Type.DELAYED, detectedAt, original, newAt);
        }
    }

    /** The same episode moved earlier. */
    private static void onPulledForward(MediaEntity stored, MediaEntity fresh, Instant newAt) {
        ScheduleNote.Type current = stored.getScheduleNoteType();
        if (current == null) {
            return;
        }
        Instant original = stored.getScheduleNoteOriginalAt();
        boolean backOnSchedule = current == ScheduleNote.Type.DELAYED
                ? Duration.between(original, newAt).compareTo(minDelay(fresh)) < 0
                : Duration.between(original, newAt).compareTo(BREAK_GAP) <= 0;
        if (backOnSchedule) {
            clearNote(stored);
        } else {
            stored.setScheduleNoteExpiresAt(newAt);
        }
    }

    private static Duration minDelay(MediaEntity fresh) {
        return NOT_YET_RELEASED.equals(fresh.getStatus()) ? MIN_PREMIERE_DELAY : MIN_DELAY;
    }

    private static void clearIfExpired(MediaEntity stored, Instant now) {
        Instant expiresAt = stored.getScheduleNoteExpiresAt();
        if (stored.getScheduleNoteType() != null && expiresAt != null && !expiresAt.isAfter(now)) {
            clearNote(stored);
        }
    }

    private static void setNote(MediaEntity stored, ScheduleNote.Type type, Instant detectedAt,
                                Instant originalAt, Instant expiresAt) {
        stored.setScheduleNoteType(type);
        stored.setScheduleNoteDetectedAt(detectedAt);
        stored.setScheduleNoteOriginalAt(originalAt);
        stored.setScheduleNoteExpiresAt(expiresAt);
    }

    private static void clearNote(MediaEntity stored) {
        setNote(stored, null, null, null, null);
    }
}
