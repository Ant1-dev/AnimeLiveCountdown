package com.Toine.animeCountdownBackend.models.discovery;

import java.time.Instant;

/** An episode airing around now. The browser decides "airing now" vs "next hour" from its own clock. */
public record LiveEpisode(Long mediaId, String engtitle, String romtitle, String coverimage,
                          Integer episode, Instant airingAt, int durationMinutes) {
}
