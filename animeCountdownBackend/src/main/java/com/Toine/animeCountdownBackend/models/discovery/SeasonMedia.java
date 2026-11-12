package com.Toine.animeCountdownBackend.models.discovery;

import java.time.Instant;
import java.util.List;

/**
 * One show in the season browser. Field names match the media_info JSON the frontend already
 * uses (MediaInfo model), so cards can share code.
 */
public record SeasonMedia(
        Long id,
        String engtitle,
        String romtitle,
        String coverimage,
        List<String> genres,
        Integer avgscore,
        Long popularity,
        Integer totalepisodes,
        String status,
        String format,
        Integer nextepisode,
        Instant airingat) {
}
