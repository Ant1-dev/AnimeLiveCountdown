package com.Toine.animeCountdownBackend.models.discovery;

import java.time.Instant;
import java.util.List;

/** Episodes airing between {@code from} and {@code to}, soonest first. */
public record LiveResponse(Instant generatedAt, Instant from, Instant to, List<LiveEpisode> episodes) {
}
