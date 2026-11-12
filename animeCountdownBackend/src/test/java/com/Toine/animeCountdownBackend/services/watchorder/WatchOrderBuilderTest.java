package com.Toine.animeCountdownBackend.services.watchorder;

import com.Toine.animeCountdownBackend.services.watchorder.RelationGraph.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WatchOrderBuilderTest {

    private static Node node(long id, String format, int year, Edge... edges) {
        return new Node(id, "ANIME", format, "FINISHED", 12, new FuzzyDate(year, 1, 1),
                new Titles("Show " + id, "Romaji " + id), new Cover("c" + id), new Relations(List.of(edges)));
    }

    private static Node leaf(long id, String type, String format, int year) {
        return new Node(id, type, format, "FINISHED", 1, new FuzzyDate(year, 6, 1), new Titles(null, "Romaji " + id), null, null);
    }

    private static Edge edge(String type, Node node) {
        return new Edge(type, node);
    }

    @Test
    void ordersMainLineAndSideStoriesByReleaseDate() {
        // S1 (2019) -> S2 (2021) -> S3 (2023); S1 has a 2020 movie side story; S2 a manga adaptation source
        Node s3 = node(3, "TV", 2023);
        Node movie = node(10, "MOVIE", 2020);
        Node s2 = node(2, "TV", 2021, edge("SEQUEL", s3), edge("SOURCE", leaf(99, "MANGA", "MANGA", 2015)));
        Node s1 = node(1, "TV", 2019, edge("SEQUEL", s2), edge("SIDE_STORY", movie));

        List<WatchOrderEntry> order = WatchOrderBuilder.build(WatchOrderBuilder.index(List.of(s1)), 2, Set.of(2L));

        assertEquals(List.of(1L, 10L, 2L, 3L), order.stream().map(WatchOrderEntry::id).toList());
        assertEquals("SIDE", order.get(1).kind());
        assertTrue(order.get(2).current());
        assertTrue(order.get(2).onSite());
        assertFalse(order.get(0).onSite());
    }

    @Test
    void leavesOutSpinOffsAlternativesRecapsAndMusic() {
        Node root = node(1, "TV", 2020,
                edge("SPIN_OFF", node(2, "TV", 2021)),
                edge("ALTERNATIVE", node(3, "TV", 2010)),
                edge("SUMMARY", node(4, "SPECIAL", 2020)),
                edge("SEQUEL", leaf(5, "ANIME", "MUSIC", 2021)));

        List<WatchOrderEntry> order = WatchOrderBuilder.build(WatchOrderBuilder.index(List.of(root)), 1, Set.of());

        assertEquals(List.of(1L), order.stream().map(WatchOrderEntry::id).toList());
    }

    @Test
    void sideStoryLeadsBackToMainLineThroughParent() {
        Node main = node(1, "TV", 2018);
        Node ova = node(7, "OVA", 2019, edge("PARENT", main));

        List<WatchOrderEntry> order = WatchOrderBuilder.build(WatchOrderBuilder.index(List.of(ova)), 7, Set.of());

        assertEquals(List.of(1L, 7L), order.stream().map(WatchOrderEntry::id).toList());
        assertEquals("MAIN", order.get(0).kind());
    }

    @Test
    void unannouncedDatesGoLastAndCyclesTerminate() {
        Node s2 = new Node(2L, "ANIME", "TV", "NOT_YET_RELEASED", null, new FuzzyDate(null, null, null),
                new Titles("Season 2", null), null, null);
        Node s1 = node(1, "TV", 2024, edge("SEQUEL", s2));
        // A cycle back to the root must not loop forever
        Node prequel = node(0, "TV", 2022, edge("SEQUEL", s1));
        Node rootWithCycle = node(1, "TV", 2024, edge("SEQUEL", s2), edge("PREQUEL", prequel));

        Map<Long, Node> byId = WatchOrderBuilder.index(List.of(rootWithCycle));
        List<WatchOrderEntry> order = WatchOrderBuilder.build(byId, 1, Set.of());

        assertEquals(List.of(0L, 1L, 2L), order.stream().map(WatchOrderEntry::id).toList());
        assertEquals(List.of(2L), WatchOrderBuilder.frontier(byId, 1));
    }
}
