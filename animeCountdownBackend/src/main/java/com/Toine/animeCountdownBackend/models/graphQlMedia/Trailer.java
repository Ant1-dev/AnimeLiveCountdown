package com.Toine.animeCountdownBackend.models.graphQlMedia;

/** AniList MediaTrailer: a YouTube or Dailymotion video id plus a thumbnail URL. */
public record Trailer(String id, String site, String thumbnail) {
}
