package com.Toine.animeCountdownBackend.services.notifications;

import java.time.Duration;
import java.time.Instant;

/**
 * Decides whether a reminder is due.
 *
 * A reminder with lead time L for an episode airing at A should go out at A - L. The job runs every
 * {@code early} (30 s by default), so a reminder counts as due once its send time is within one
 * run of now. That way "at airing" reminders are always sent before the just-aired sync moves the
 * show on to its next episode. Reminders that were missed (created inside the lead window, or the
 * server was down) are still sent until {@code grace} after the episode aired.
 */
public final class DueEpisodeSelector {

    private final Duration early;
    private final Duration grace;

    public DueEpisodeSelector(Duration early, Duration grace) {
        this.early = early;
        this.grace = grace;
    }

    public boolean isDue(Instant airingAt, int leadMinutes, Instant now) {
        if (airingAt == null) {
            return false;
        }
        Instant sendAt = airingAt.minus(Duration.ofMinutes(Math.max(0, leadMinutes)));
        boolean sendTimeReached = !now.plus(early).isBefore(sendAt);
        boolean notLongPastAiring = airingAt.isAfter(now.minus(grace));
        return sendTimeReached && notLongPastAiring;
    }

    public boolean isDue(ReminderCandidate candidate, Instant now) {
        return candidate.episode() != null && isDue(candidate.airingAt(), candidate.leadMinutes(), now);
    }

    /** Earliest airing time a due reminder can have at {@code now}. */
    public Instant windowStart(Instant now) {
        return now.minus(grace);
    }

    /** Latest airing time a due reminder can have at {@code now}, for the longest allowed lead. */
    public Instant windowEnd(Instant now, int maxLeadMinutes) {
        return now.plus(early).plus(Duration.ofMinutes(maxLeadMinutes));
    }

    /** Text for the notification, worded for before or after airing. */
    public static String describe(int episode, Instant airingAt, Instant now) {
        long secondsLeft = Duration.between(now, airingAt).getSeconds();
        if (secondsLeft <= 60) {
            return "Episode " + episode + " is out now";
        }
        // Reminders go out up to one run (30 s) early, so round to the nearest minute, halves down:
        // a 15-minute reminder sent with 15:30 left still says 15 minutes
        long minutes = (secondsLeft + 29) / 60;
        String in;
        if (minutes < 60) {
            in = minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            long hours = Math.round(minutes / 60.0);
            in = hours + (hours == 1 ? " hour" : " hours");
        }
        return "Episode " + episode + " airs in " + in;
    }
}
