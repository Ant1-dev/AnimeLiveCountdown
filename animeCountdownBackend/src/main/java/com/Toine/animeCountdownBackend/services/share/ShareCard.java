package com.Toine.animeCountdownBackend.services.share;

/**
 * Everything a link preview for one show needs.
 *
 * @param countdown e.g. "Ep 5 airs in 2d 4h"
 * @param airingLine absolute airing time in UTC, or null
 * @param synopsis plain-text description, possibly empty
 */
public record ShareCard(long mediaId, String title, String countdown, String airingLine, String synopsis,
                        String coverUrl, String bannerUrl) {
}
