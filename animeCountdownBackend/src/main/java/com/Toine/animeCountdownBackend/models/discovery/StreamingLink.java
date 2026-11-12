package com.Toine.animeCountdownBackend.models.discovery;

/** A legal streaming site for a show, taken from AniList's externalLinks (type STREAMING). */
public record StreamingLink(String site, String url, String icon, String color, String language) {
}
