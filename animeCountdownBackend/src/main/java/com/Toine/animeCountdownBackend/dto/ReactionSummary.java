package com.Toine.animeCountdownBackend.dto;

import com.Toine.animeCountdownBackend.models.ReactionType;

import java.util.Map;

/**
 * Public reaction counts for one episode.
 *
 * @param episode the episode the counts are for; null when no episode of the show has aired yet
 * @param counts  count per reaction, with every reaction type present (zero if unused)
 * @param mine    the signed-in viewer's reaction, or null
 * @param emojis  the emoji shown for each reaction type
 */
public record ReactionSummary(Long mediaId, Integer episode, Map<ReactionType, Long> counts, long total,
                              ReactionType mine, Map<ReactionType, String> emojis) {
}
