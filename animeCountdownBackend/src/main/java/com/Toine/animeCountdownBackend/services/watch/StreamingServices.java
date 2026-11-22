package com.Toine.animeCountdownBackend.services.watch;

import com.Toine.animeCountdownBackend.models.discovery.StreamingLink;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * The streaming services a user can pick as "My services", and how the names used by our two data
 * sources map onto them: AniList externalLinks sites ("Amazon Prime Video", "Disney Plus", "Bilibili TV")
 * and AnimeSchedule stream platform keys ("amazon", "disney", "bilibili").
 *
 * <p>Built from the names that actually appear in media_info.streaming_links and release_time.streams.
 * Pure functions (unit tested).
 */
public final class StreamingServices {

    /**
     * A pickable service. {@code key} is what's stored on the account; {@code aliases} are the source
     * names that mean it (the frontend matches them the same way as {@link #keyFor}).
     */
    public record Service(String key, String label, String color, List<String> aliases) {
    }

    /** Where a show streams on one service. */
    public record ServiceLink(String key, String url) {
    }

    private record Entry(Service service, List<String> aliases) {
    }

    private static final List<Entry> CATALOG = List.of(
            entry("crunchyroll", "Crunchyroll", "#F47521", "Crunchyroll", "Funimation"),
            entry("netflix", "Netflix", "#E50914", "Netflix"),
            entry("hidive", "HIDIVE", "#00AEEF", "HIDIVE"),
            entry("hulu", "Hulu", "#1CE783", "Hulu"),
            entry("disney", "Disney+", "#113CCF", "Disney", "Disney Plus", "Disney+"),
            entry("amazon", "Prime Video", "#00A8E1", "Amazon", "Amazon Prime Video", "Prime Video", "Amazon Prime"),
            entry("apple", "Apple TV", "#A2AAAD", "Apple", "Apple TV", "Apple TV+"),
            entry("max", "Max", "#002BE7", "Max", "HBO Max", "HBO"),
            entry("youtube", "YouTube", "#FF0000", "YouTube", "Muse Asia", "Ani-One Asia"),
            entry("bilibili", "Bilibili", "#00A1D6", "Bilibili", "Bilibili TV"),
            entry("oceanveil", "OceanVeil", "#2B6CB0", "OceanVeil"),
            entry("tubi", "Tubi", "#FA541C", "Tubi", "Tubi TV"),
            entry("adultswim", "Adult Swim", "#7C7C7C", "Adult Swim"),
            entry("retrocrush", "RetroCrush", "#E4002B", "RetroCrush"),
            entry("crackle", "Crackle", "#F5A623", "Crackle"),
            entry("hoopla", "Hoopla", "#1B75BC", "Hoopla"),
            entry("iqiyi", "iQIYI", "#00CC36", "iQIYI", "iQ"),
            entry("tencent", "Tencent Video", "#FF6A00", "Tencent Video", "Tencent"),
            entry("wetv", "WeTV", "#FF4A4A", "WeTV"),
            entry("youku", "Youku", "#1F8FFF", "Youku", "Youku TV"));

    private static final Map<String, String> KEY_BY_ALIAS = new HashMap<>();
    private static final Map<String, Service> BY_KEY = new LinkedHashMap<>();
    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        for (Entry e : CATALOG) {
            BY_KEY.put(e.service().key(), e.service());
            KEY_BY_ALIAS.put(simplify(e.service().key()), e.service().key());
            for (String alias : e.aliases()) {
                KEY_BY_ALIAS.put(simplify(alias), e.service().key());
            }
        }
    }

    private StreamingServices() {
    }

    private static Entry entry(String key, String label, String color, String... aliases) {
        return new Entry(new Service(key, label, color, List.of(aliases)), List.of(aliases));
    }

    /** Every pickable service, in display order. */
    public static List<Service> all() {
        return List.copyOf(BY_KEY.values());
    }

    public static boolean isKnown(String key) {
        return key != null && BY_KEY.containsKey(key);
    }

    /**
     * The service key for an AniList site name or an AnimeSchedule platform key, or empty for sites
     * outside the catalog. Case, spaces and punctuation are ignored ("Disney Plus" = "disney+").
     */
    public static Optional<String> keyFor(String siteOrPlatform) {
        if (siteOrPlatform == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KEY_BY_ALIAS.get(simplify(siteOrPlatform)));
    }

    /**
     * Which services a show streams on, from its AniList links and AnimeSchedule streams JSON
     * ({"crunchyroll": "https://..."} per release_time row), in catalog order with one link each
     * (AniList's English or language-less link first). Empty when the show has streaming data but none
     * of it is on a catalog service; callers tell "no data" apart by whether there was any input.
     */
    public static List<ServiceLink> linksFor(List<StreamingLink> aniListLinks, Collection<String> streamsJson) {
        Map<String, String> urls = new HashMap<>();
        if (aniListLinks != null) {
            List<StreamingLink> preferred = new ArrayList<>(aniListLinks);
            preferred.sort(Comparator.comparing(l -> isEnglish(l.language()) ? 0 : 1));
            for (StreamingLink link : preferred) {
                if (link.url() != null && !link.url().isBlank()) {
                    keyFor(link.site()).ifPresent(k -> urls.putIfAbsent(k, link.url()));
                }
            }
        }
        if (streamsJson != null) {
            for (String json : streamsJson) {
                streams(json).forEach((platform, url) -> keyFor(platform).ifPresent(k -> urls.putIfAbsent(k, url)));
            }
        }
        List<ServiceLink> out = new ArrayList<>();
        for (String key : BY_KEY.keySet()) {
            if (urls.containsKey(key)) {
                out.add(new ServiceLink(key, urls.get(key)));
            }
        }
        return out;
    }

    private static boolean isEnglish(String language) {
        return language == null || language.isBlank() || language.equalsIgnoreCase("English");
    }

    /** Platform -> URL in a release_time.streams JSON object; empty for blank or bad JSON. */
    static Map<String, String> streams(String streamsJson) {
        Map<String, String> out = new LinkedHashMap<>();
        if (streamsJson == null || streamsJson.isBlank()) {
            return out;
        }
        try {
            Map<String, Object> map = JSON.readValue(streamsJson, new TypeReference<Map<String, Object>>() { });
            map.forEach((platform, url) -> {
                if (url instanceof String u && !u.isBlank()) {
                    out.put(platform, u);
                }
            });
        } catch (Exception e) {
            // Bad JSON counts as no streams
        }
        return out;
    }

    static String simplify(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
            }
        }
        // "Disney+" -> "disneyplus", matching AniList's "Disney Plus"
        if (name.trim().endsWith("+")) {
            out.append("plus");
        }
        return out.toString();
    }
}
