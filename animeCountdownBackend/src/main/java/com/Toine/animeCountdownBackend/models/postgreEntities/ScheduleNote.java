package com.Toine.animeCountdownBackend.models.postgreEntities;

import java.time.Instant;

/**
 * A note that a show is off its usual schedule, as sent to clients.
 *
 * @param type       ON_BREAK (the next episode is much further out than the show's usual cadence)
 *                   or DELAYED (an announced episode was pushed back)
 * @param detectedAt when the sync noticed it
 * @param originalAt ON_BREAK: when the previous episode aired. DELAYED: the originally announced time.
 * @param expiresAt  the episode's new airing time; the note no longer applies after it
 */
public record ScheduleNote(Type type, Instant detectedAt, Instant originalAt, Instant expiresAt) {

    public enum Type { ON_BREAK, DELAYED }

    /** The entity's note if it has one that hasn't expired yet, otherwise null. */
    public static ScheduleNote activeOf(MediaEntity media, Instant now) {
        Type type = media.getScheduleNoteType();
        Instant expiresAt = media.getScheduleNoteExpiresAt();
        if (type == null || (expiresAt != null && !expiresAt.isAfter(now))) {
            return null;
        }
        return new ScheduleNote(type, media.getScheduleNoteDetectedAt(), media.getScheduleNoteOriginalAt(), expiresAt);
    }
}
