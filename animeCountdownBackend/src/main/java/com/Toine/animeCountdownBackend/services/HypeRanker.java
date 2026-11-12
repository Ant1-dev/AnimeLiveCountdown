package com.Toine.animeCountdownBackend.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ranks shows for "Most hyped this week":
 * <pre>
 * score = 3 × reactions in the window + 2 × new followers in the window + 1 × total followers
 * </pre>
 * Shows with a score of 0 are dropped; ties go to the more popular show.
 */
public final class HypeRanker {

    public static final int REACTION_WEIGHT = 3;
    public static final int NEW_FOLLOWER_WEIGHT = 2;
    public static final int FOLLOWER_WEIGHT = 1;

    private HypeRanker() {
    }

    public record Ranked(long mediaId, long score, long recentReactions, long newFollowers, long followers) {
    }

    /**
     * @param recentReactions reactions left in the window, per show id
     * @param newFollowers    follows added in the window, per show id
     * @param followers       total follows, per show id
     * @param eligible        which shows may be ranked (e.g. currently airing)
     * @param popularity      tie-breaker, higher first (AniList popularity)
     */
    public static List<Ranked> rank(Map<Long, Long> recentReactions, Map<Long, Long> newFollowers,
                                    Map<Long, Long> followers, Predicate<Long> eligible,
                                    Map<Long, Long> popularity, int limit) {
        Set<Long> ids = new HashSet<>(recentReactions.keySet());
        ids.addAll(newFollowers.keySet());
        ids.addAll(followers.keySet());

        Comparator<Ranked> order = Comparator.comparingLong(Ranked::score).reversed()
                .thenComparing(r -> popularity.getOrDefault(r.mediaId(), 0L), Comparator.reverseOrder())
                .thenComparingLong(Ranked::mediaId);

        return ids.stream()
                .filter(eligible)
                .map(id -> {
                    long reactions = recentReactions.getOrDefault(id, 0L);
                    long fresh = newFollowers.getOrDefault(id, 0L);
                    long total = followers.getOrDefault(id, 0L);
                    long score = REACTION_WEIGHT * reactions + NEW_FOLLOWER_WEIGHT * fresh + FOLLOWER_WEIGHT * total;
                    return new Ranked(id, score, reactions, fresh, total);
                })
                .filter(r -> r.score() > 0)
                .sorted(order)
                .limit(Math.max(0, limit))
                .toList();
    }
}
