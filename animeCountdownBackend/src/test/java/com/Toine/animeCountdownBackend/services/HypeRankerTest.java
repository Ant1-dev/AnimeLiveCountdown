package com.Toine.animeCountdownBackend.services;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HypeRankerTest {

    @Test
    void scoresWeightReactionsThenNewFollowersThenFollowers() {
        List<HypeRanker.Ranked> ranked = HypeRanker.rank(
                Map.of(1L, 2L),          // show 1: 2 recent reactions                -> 3×2       = 6
                Map.of(2L, 2L),          // show 2: 2 new followers, 3 followers total -> 2×2 + 1×3 = 7
                Map.of(2L, 3L, 3L, 5L),  // show 3: 5 followers                        -> 1×5       = 5
                id -> true, Map.of(), 10);

        assertEquals(List.of(2L, 1L, 3L), ranked.stream().map(HypeRanker.Ranked::mediaId).toList());
        assertEquals(new HypeRanker.Ranked(2L, 7, 0, 2, 3), ranked.get(0));
        assertEquals(new HypeRanker.Ranked(1L, 6, 2, 0, 0), ranked.get(1));
        assertEquals(new HypeRanker.Ranked(3L, 5, 0, 0, 5), ranked.get(2));
    }

    @Test
    void ineligibleShowsAreDropped() {
        List<HypeRanker.Ranked> ranked = HypeRanker.rank(
                Map.of(1L, 10L, 2L, 1L), Map.of(), Map.of(),
                id -> id != 1L, Map.of(), 10);

        assertEquals(List.of(2L), ranked.stream().map(HypeRanker.Ranked::mediaId).toList());
    }

    @Test
    void tiesGoToTheMorePopularShow() {
        List<HypeRanker.Ranked> ranked = HypeRanker.rank(
                Map.of(1L, 1L, 2L, 1L, 3L, 1L), Map.of(), Map.of(),
                id -> true, Map.of(1L, 100L, 2L, 900L, 3L, 500L), 10);

        assertEquals(List.of(2L, 3L, 1L), ranked.stream().map(HypeRanker.Ranked::mediaId).toList());
    }

    @Test
    void zeroScoresAreDroppedAndLimitApplies() {
        List<HypeRanker.Ranked> ranked = HypeRanker.rank(
                Map.of(1L, 0L, 2L, 3L, 3L, 2L, 4L, 1L), Map.of(), Map.of(),
                id -> true, Map.of(), 2);

        assertEquals(List.of(2L, 3L), ranked.stream().map(HypeRanker.Ranked::mediaId).toList());
    }

    @Test
    void emptyInputGivesEmptyRanking() {
        assertTrue(HypeRanker.rank(Map.of(), Map.of(), Map.of(), id -> true, Map.of(), 10).isEmpty());
    }
}
