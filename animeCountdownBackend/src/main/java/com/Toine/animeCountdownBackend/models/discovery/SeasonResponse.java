package com.Toine.animeCountdownBackend.models.discovery;

import java.time.LocalDate;
import java.util.List;

/**
 * @param source  "local" (served from media_info) or "anilist" (fetched on demand and cached)
 * @param genres  every genre present in the season, for the filter dropdown (before filtering)
 */
public record SeasonResponse(String season, int year, LocalDate startsAt, String source,
                             List<String> genres, List<SeasonMedia> media) {
}
