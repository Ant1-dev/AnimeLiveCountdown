package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing AnimeSchedule data and choosing which release to show. Pure functions (unit tested). */
public final class ReleaseTimes {
    private static final Pattern ANILIST_URL = Pattern.compile("anilist\\.co/anime/(\\d+)");
    private static final ObjectMapper JSON = new ObjectMapper();
    /** An episode released this recently still counts as "the" release (it's out now). */
    static final Duration JUST_RELEASED = Duration.ofHours(2);

    private static final Map<String, String> PLATFORM_LABELS = Map.of(
            "crunchyroll", "Crunchyroll", "funimation", "Funimation", "wakanim", "Wakanim",
            "amazon", "Prime Video", "hidive", "HIDIVE", "hulu", "Hulu", "youtube", "YouTube",
            "netflix", "Netflix");

    private ReleaseTimes() {
    }

    public record Stream(String platform, String label, String url) {
    }

    /** One release to show: the next one, or the latest if nothing is scheduled. */
    public record Release(String type, int episode, Instant releaseAt, List<Stream> streams,
                          Instant delayedUntil, String delayNote) {
    }

    /** From an AnimeSchedule websites.aniList value such as "anilist.co/anime/154587". */
    public static Optional<Long> aniListId(Map<String, String> websites) {
        if (websites == null) {
            return Optional.empty();
        }
        String value = websites.get("aniList");
        if (value == null) {
            return Optional.empty();
        }
        Matcher m = ANILIST_URL.matcher(value);
        return m.find() ? Optional.of(Long.parseLong(m.group(1))) : Optional.empty();
    }

    /** ISO-8601 date-time, or empty for blanks and AnimeSchedule's "0001-01-01" null date. */
    public static Optional<Instant> instant(String value) {
        if (value == null || value.isBlank() || value.startsWith("0001-")) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException e) {
            try {
                return Optional.of(Instant.parse(value));
            } catch (DateTimeParseException e2) {
                return Optional.empty();
            }
        }
    }

    public static Optional<String> airType(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "raw" -> Optional.of("RAW");
            case "sub" -> Optional.of("SUB");
            case "dub" -> Optional.of("DUB");
            default -> Optional.empty();
        };
    }

    /** Platform -> https URL, dropping blanks. AnimeSchedule sends URLs without a scheme. */
    public static Map<String, String> normalizeStreams(Map<String, String> streams) {
        Map<String, String> out = new TreeMap<>();
        if (streams == null) {
            return out;
        }
        streams.forEach((platform, url) -> {
            if (platform != null && url != null && !url.isBlank()) {
                String u = url.trim();
                out.put(platform, u.startsWith("http://") || u.startsWith("https://") ? u : "https://" + u.replaceFirst("^/+", ""));
            }
        });
        return out;
    }

    public static String streamsToJson(Map<String, String> streams) {
        try {
            return JSON.writeValueAsString(streams);
        } catch (Exception e) {
            return "{}";
        }
    }

    static List<Stream> streamsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Map<String, String> map = JSON.readValue(json, new TypeReference<TreeMap<String, String>>() { });
            List<Stream> streams = new ArrayList<>();
            map.forEach((platform, url) -> streams.add(new Stream(platform,
                    PLATFORM_LABELS.getOrDefault(platform, Character.toUpperCase(platform.charAt(0)) + platform.substring(1)), url)));
            return streams;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Per type (RAW/SUB/DUB): the next release (or one that just came out), otherwise the most recent.
     */
    public static Map<String, Release> pick(List<ReleaseTimeEntity> rows, Instant now) {
        Map<String, ReleaseTimeEntity> chosen = new HashMap<>();
        Instant cutoff = now.minus(JUST_RELEASED);
        for (ReleaseTimeEntity r : rows) {
            ReleaseTimeEntity best = chosen.get(r.getAirType());
            if (best == null || better(r, best, cutoff)) {
                chosen.put(r.getAirType(), r);
            }
        }
        Map<String, Release> out = new TreeMap<>();
        chosen.forEach((type, r) -> out.put(type, new Release(type, r.getEpisode(), r.getReleaseAt(),
                streamsFromJson(r.getStreamsJson()),
                r.getDelayedUntil() != null && r.getDelayedUntil().isAfter(now) ? r.getDelayedUntil() : null,
                r.getDelayedUntil() != null && r.getDelayedUntil().isAfter(now) ? r.getDelayNote() : null)));
        return out;
    }

    private static boolean better(ReleaseTimeEntity candidate, ReleaseTimeEntity best, Instant cutoff) {
        boolean candidateUpcoming = !candidate.getReleaseAt().isBefore(cutoff);
        boolean bestUpcoming = !best.getReleaseAt().isBefore(cutoff);
        if (candidateUpcoming != bestUpcoming) {
            return candidateUpcoming; // anything upcoming beats anything past
        }
        // Upcoming: the soonest. Past: the latest.
        return candidateUpcoming ? candidate.getReleaseAt().isBefore(best.getReleaseAt())
                : candidate.getReleaseAt().isAfter(best.getReleaseAt());
    }
}
