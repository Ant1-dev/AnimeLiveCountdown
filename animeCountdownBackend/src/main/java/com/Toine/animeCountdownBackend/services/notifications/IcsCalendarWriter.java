package com.Toine.animeCountdownBackend.services.notifications;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Writes an iCalendar (RFC 5545) feed of episodes. Times are written in UTC ("...Z"), which every
 * calendar app converts to the viewer's own timezone, so no VTIMEZONE block is needed.
 */
public final class IcsCalendarWriter {

    public record Episode(long mediaId, int episode, String title, Instant airingAt, int durationMinutes) {
    }

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String CRLF = "\r\n";
    private static final int MAX_LINE_OCTETS = 75;

    private final String siteUrl;
    private final String uidDomain;

    public IcsCalendarWriter(String siteUrl, String uidDomain) {
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
        this.uidDomain = uidDomain;
    }

    public String write(List<Episode> episodes, Instant now) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Anime Live Countdown//Episode Calendar//EN");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:Anime Live Countdown");
        line(out, "X-WR-CALDESC:" + escape("Upcoming episodes of the shows you follow"));
        line(out, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
        line(out, "X-PUBLISHED-TTL:PT1H");

        String stamp = UTC.format(now.truncatedTo(ChronoUnit.SECONDS));
        episodes.stream()
                .filter(e -> e.airingAt() != null)
                .sorted(Comparator.comparing(Episode::airingAt).thenComparing(Episode::mediaId))
                .forEach(e -> writeEvent(out, e, stamp));

        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private void writeEvent(StringBuilder out, Episode e, String stamp) {
        String link = siteUrl + "/media/" + e.mediaId();
        int duration = e.durationMinutes() > 0 ? e.durationMinutes() : 24;
        line(out, "BEGIN:VEVENT");
        line(out, "UID:" + e.mediaId() + "-" + e.episode() + "@" + uidDomain);
        line(out, "DTSTAMP:" + stamp);
        line(out, "DTSTART:" + UTC.format(e.airingAt()));
        line(out, "DTEND:" + UTC.format(e.airingAt().plus(duration, ChronoUnit.MINUTES)));
        line(out, "SUMMARY:" + escape(e.title() + " - Episode " + e.episode()));
        line(out, "DESCRIPTION:" + escape("Episode " + e.episode() + " of " + e.title() + "\n" + link));
        line(out, "URL:" + link);
        line(out, "TRANSP:TRANSPARENT");
        line(out, "END:VEVENT");
    }

    /** Escapes a TEXT value (RFC 5545 section 3.3.11). */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /** Appends a content line, folded so no physical line exceeds 75 octets (section 3.1). */
    static void line(StringBuilder out, String content) {
        int octets = 0;
        int limit = MAX_LINE_OCTETS;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > limit) {
                out.append(CRLF).append(' ');
                octets = 0;
                limit = MAX_LINE_OCTETS - 1; // the leading space counts toward the next line
            }
            out.appendCodePoint(codePoint);
            octets += size;
            i += charCount;
        }
        out.append(CRLF);
    }
}
