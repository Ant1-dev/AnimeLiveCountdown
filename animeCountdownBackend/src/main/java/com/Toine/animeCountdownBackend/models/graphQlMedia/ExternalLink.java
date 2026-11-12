package com.Toine.animeCountdownBackend.models.graphQlMedia;

/** AniList MediaExternalLink (only the fields we request). */
public record ExternalLink(String site, String url, String icon, String color, String language,
                           String type, Boolean isDisabled) {
}
