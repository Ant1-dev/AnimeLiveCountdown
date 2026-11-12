package com.Toine.animeCountdownBackend.services.watchorder;

import com.Toine.animeCountdownBackend.services.watchorder.RelationGraph.Edge;
import com.Toine.animeCountdownBackend.services.watchorder.RelationGraph.FuzzyDate;
import com.Toine.animeCountdownBackend.services.watchorder.RelationGraph.Node;

import java.util.*;
import java.util.function.Predicate;

/**
 * Turns AniList relation graphs into a release-ordered watch list.
 *
 * Starting from the show being viewed it follows PREQUEL/SEQUEL (the main line), PARENT (back to the
 * main line from a side story) and SIDE_STORY (movies, OVAs, specials). Spin-offs, alternative
 * versions, recaps, music videos and non-anime sources (manga, novels) are left out.
 */
public final class WatchOrderBuilder {
    static final int MAX_ENTRIES = 40;

    private WatchOrderBuilder() {
    }

    /**
     * Every node in the graphs, keyed by id. A show can appear several times in a nested response, and
     * some copies come back with fewer (or zero) relations, so the relations of all copies are merged.
     * A node keeps null relations only when no copy had any (it's at the edge of the query).
     */
    public static Map<Long, Node> index(Collection<Node> roots) {
        Map<Long, Node> firstCopy = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<String, Edge>> edges = new HashMap<>();
        Deque<Node> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            Node n = stack.pop();
            if (n == null || n.id() == null) {
                continue;
            }
            firstCopy.putIfAbsent(n.id(), n);
            if (n.relations() != null && n.relations().edges() != null) {
                LinkedHashMap<String, Edge> merged = edges.computeIfAbsent(n.id(), k -> new LinkedHashMap<>());
                for (Edge e : n.relations().edges()) {
                    if (e != null && e.node() != null && e.node().id() != null) {
                        merged.putIfAbsent(e.relationType() + ":" + e.node().id(), e);
                        stack.push(e.node());
                    }
                }
            }
        }
        Map<Long, Node> byId = new HashMap<>();
        firstCopy.forEach((id, n) -> {
            LinkedHashMap<String, Edge> merged = edges.get(id);
            byId.put(id, new Node(n.id(), n.type(), n.format(), n.status(), n.episodes(), n.startDate(), n.title(),
                    n.coverImage(), merged == null ? null : new RelationGraph.Relations(new ArrayList<>(merged.values()))));
        });
        return byId;
    }

    /** Ids reachable in the franchise whose relations haven't been fetched yet (to query next). */
    public static List<Long> frontier(Map<Long, Node> byId, long rootId) {
        List<Long> unexplored = new ArrayList<>();
        walk(byId, rootId, n -> {
            if (n.relations() == null) {
                unexplored.add(n.id());
            }
            return true;
        });
        return unexplored;
    }

    public static List<WatchOrderEntry> build(Map<Long, Node> byId, long rootId, Set<Long> onSite) {
        Map<Long, String> kinds = walk(byId, rootId, n -> true);
        List<Node> nodes = new ArrayList<>();
        for (Long id : kinds.keySet()) {
            Node n = byId.get(id);
            if (n != null) {
                nodes.add(n);
            }
        }
        nodes.sort(Comparator.comparing((Node n) -> dateKey(n.startDate())).thenComparing(Node::id));
        List<WatchOrderEntry> entries = new ArrayList<>();
        for (Node n : nodes) {
            entries.add(new WatchOrderEntry(n.id(), title(n), n.format(), year(n), n.episodes(), n.status(),
                    n.coverImage() == null ? null : n.coverImage().medium(), kinds.get(n.id()),
                    n.id() == rootId, onSite.contains(n.id())));
        }
        return entries;
    }

    /** A relation as seen from one side: A -SEQUEL-> B is also B -PREQUEL-> A. */
    private record Link(String type, Node target) {
    }

    /**
     * Relations in both directions. AniList usually lists both, but a node fetched at the edge of a
     * query has none of its own, so its links back are rebuilt from the other side.
     */
    private static Map<Long, List<Link>> adjacency(Map<Long, Node> byId) {
        Map<Long, List<Link>> adj = new HashMap<>();
        for (Node n : byId.values()) {
            if (n.relations() == null || n.relations().edges() == null) {
                continue;
            }
            for (Edge e : n.relations().edges()) {
                if (e == null || e.node() == null || e.node().id() == null || e.relationType() == null) {
                    continue;
                }
                Node target = byId.getOrDefault(e.node().id(), e.node());
                adj.computeIfAbsent(n.id(), k -> new ArrayList<>()).add(new Link(e.relationType(), target));
                String inverse = switch (e.relationType()) {
                    case "SEQUEL" -> "PREQUEL";
                    case "PREQUEL" -> "SEQUEL";
                    case "SIDE_STORY" -> "PARENT";
                    case "PARENT" -> "SIDE_STORY";
                    default -> null;
                };
                if (inverse != null) {
                    adj.computeIfAbsent(target.id(), k -> new ArrayList<>()).add(new Link(inverse, n));
                }
            }
        }
        return adj;
    }

    /** Breadth-first walk of the franchise from the root; returns each visited id's kind (MAIN/SIDE). */
    private static Map<Long, String> walk(Map<Long, Node> byId, long rootId, Predicate<Node> visitor) {
        Map<Long, String> kinds = new LinkedHashMap<>();
        if (!byId.containsKey(rootId)) {
            return kinds;
        }
        Map<Long, List<Link>> adj = adjacency(byId);
        Deque<Long> queue = new ArrayDeque<>();
        kinds.put(rootId, "MAIN");
        queue.add(rootId);
        while (!queue.isEmpty() && kinds.size() < MAX_ENTRIES) {
            Node n = byId.get(queue.poll());
            if (n == null || !visitor.test(n)) {
                continue;
            }
            String kind = kinds.get(n.id());
            for (Link link : adj.getOrDefault(n.id(), List.of())) {
                Node target = link.target();
                if (kinds.containsKey(target.id()) || !"ANIME".equals(target.type()) || "MUSIC".equals(target.format())) {
                    continue;
                }
                String next = switch (link.type()) {
                    case "PREQUEL", "SEQUEL" -> kind;
                    case "PARENT" -> "MAIN";
                    case "SIDE_STORY" -> "SIDE";
                    default -> null;
                };
                if (next != null && kinds.size() < MAX_ENTRIES) {
                    kinds.put(target.id(), next);
                    queue.add(target.id());
                }
            }
        }
        return kinds;
    }

    private static String title(Node n) {
        if (n.title() == null) {
            return "Anime #" + n.id();
        }
        String english = n.title().english();
        return english != null && !english.isBlank() ? english : (n.title().romaji() == null ? "Anime #" + n.id() : n.title().romaji());
    }

    private static Integer year(Node n) {
        return n.startDate() == null ? null : n.startDate().year();
    }

    /** Sorts by release date; unknown dates (unannounced sequels) go last. */
    private static long dateKey(FuzzyDate d) {
        if (d == null || d.year() == null) {
            return Long.MAX_VALUE;
        }
        int month = d.month() == null ? 12 : d.month();
        int day = d.day() == null ? 31 : d.day();
        return d.year() * 10_000L + month * 100L + day;
    }
}
