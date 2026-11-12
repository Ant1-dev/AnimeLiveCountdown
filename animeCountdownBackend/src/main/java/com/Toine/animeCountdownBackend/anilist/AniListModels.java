package com.Toine.animeCountdownBackend.anilist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** AniList API shapes used by the connect + import flow. */
public final class AniListModels {
    private AniListModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** One entry on the user's AniList list (MediaList); null when the show isn't on it. */
    public record MediaListEntry(Long id, String status, Integer progress) {
    }

    public record Viewer(Long id, String name) {
    }

    /** MediaListCollection: the user's lists (one per status, plus custom lists). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListCollection(List<ListGroup> lists) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListGroup(String name, List<ListEntry> entries) {
    }

    /** One list entry: CURRENT or PLANNING, and how many episodes the user has watched. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListEntry(String status, Integer progress, ListMedia media) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListMedia(
            Long id,
            @JsonProperty("isAdult") Boolean adult,
            String status,
            Integer episodes,
            Long popularity,
            Integer seasonYear,
            String season,
            String bannerImage,
            Title title,
            NextAiring nextAiringEpisode,
            Cover coverImage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Title(String english, String romaji) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NextAiring(Long airingAt, Integer episode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(String large) {
    }
}
