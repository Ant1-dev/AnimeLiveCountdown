package com.Toine.animeCountdownBackend.services.watchorder;

import java.util.List;

/** The slice of AniList's Media/relations graph the watch order needs (Jackson maps the JSON onto these). */
public final class RelationGraph {
    private RelationGraph() {
    }

    public record Node(Long id, String type, String format, String status, Integer episodes,
                       FuzzyDate startDate, Titles title, Cover coverImage, Relations relations) {
    }

    public record FuzzyDate(Integer year, Integer month, Integer day) {
    }

    public record Titles(String english, String romaji) {
    }

    public record Cover(String medium) {
    }

    /** Null when the node was fetched at the edge of a query (its relations weren't requested). */
    public record Relations(List<Edge> edges) {
    }

    public record Edge(String relationType, Node node) {
    }
}
