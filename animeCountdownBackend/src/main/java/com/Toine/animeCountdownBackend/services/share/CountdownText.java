package com.Toine.animeCountdownBackend.services.share;

import java.time.Duration;
import java.time.Instant;

/** Short countdown phrases for share previews, e.g. "Ep 5 airs in 2d 4h". */
public final class CountdownText {

    private CountdownText() {
    }

    public static String line(String status, Integer nextEpisode, Instant nextAiringAt, Integer totalEpisodes,
                              Instant now) {
        if (nextAiringAt != null && nextAiringAt.isAfter(now)) {
            String in = "airs in " + duration(Duration.between(now, nextAiringAt));
            return nextEpisode != null ? "Ep " + nextEpisode + " " + in : "Airs in " + in.substring(8);
        }
        if (nextAiringAt != null && nextEpisode != null && "RELEASING".equals(status)) {
            return "Ep " + nextEpisode + " is out now";
        }
        if ("FINISHED".equals(status)) {
            return totalEpisodes != null && totalEpisodes > 0
                    ? "Finished airing · " + totalEpisodes + (totalEpisodes == 1 ? " episode" : " episodes")
                    : "Finished airing";
        }
        if ("NOT_YET_RELEASED".equals(status)) {
            return "Coming soon";
        }
        if ("HIATUS".equals(status)) {
            return "On hiatus";
        }
        if ("CANCELLED".equals(status)) {
            return "Cancelled";
        }
        return "Airing date TBA";
    }

    /** "2d 4h", "4h 12m", "12m" (rounded down, at least 1m). */
    public static String duration(Duration d) {
        long totalMinutes = Math.max(1, d.toMinutes());
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
