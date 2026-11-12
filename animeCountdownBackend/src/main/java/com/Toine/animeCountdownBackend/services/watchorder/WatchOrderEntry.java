package com.Toine.animeCountdownBackend.services.watchorder;

/**
 * One entry in a franchise's watch order.
 *
 * @param kind    MAIN for the prequel/sequel line, SIDE for side stories (movies, OVAs, specials)
 * @param current true for the show whose page is being viewed
 * @param onSite  true when this site has a page for it; otherwise the frontend links to AniList
 */
public record WatchOrderEntry(long id, String title, String format, Integer year, Integer episodes, String status,
                              String coverImage, String kind, boolean current, boolean onSite) {
}
